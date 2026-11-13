
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.dto.allocation.AllocationRequest;
import com.employeeallocation.employee_allocation_backend.dto.allocation.AllocationResponse;
import com.employeeallocation.employee_allocation_backend.entity.*;
import com.employeeallocation.employee_allocation_backend.enums.AllocationStatus;
import com.employeeallocation.employee_allocation_backend.enums.ProjectStatus;
import com.employeeallocation.employee_allocation_backend.repository.*;
import com.employeeallocation.employee_allocation_backend.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.http.HttpStatus;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AllocationService {
    private final AppUserRepository appUserRepository;
    private final AllocationRepository allocationRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AllocationAuditLogRepository auditRepository;

    public AllocationService(
            AppUserRepository appUserRepository,
            AllocationRepository allocationRepository,
            EmployeeRepository employeeRepository,
            ProjectRepository projectRepository,
            AvailabilityRepository availabilityRepository,
            AllocationAuditLogRepository auditRepository) {

        this.allocationRepository = allocationRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.availabilityRepository = availabilityRepository;
        this.auditRepository = auditRepository;
        this.appUserRepository = appUserRepository;
    }

    // CREATE ALLOCATION
    @Transactional
    public AllocationResponse createAllocation(AllocationRequest request) {

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found"));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));

        validateRequest(request, employee, project);
        validateAvailability(employee, request);
        validateWeeklyCapacity(employee, request, null);

        Allocation allocation = new Allocation();

        allocation.setEmployee(employee);
        allocation.setProject(project);
        allocation.setStartDate(request.getStartDate());
        allocation.setEndDate(request.getEndDate());
        allocation.setAllocatedHoursPerWeek(
                request.getAllocatedHoursPerWeek());
        allocation.setStatus(AllocationStatus.PROPOSED);
        allocation.setIsDeleted(false);

        Allocation saved = allocationRepository.save(allocation);

        saveAudit(saved, "CREATED", "Allocation created");

        return mapToResponse(saved);
    }

    // GET ALL NON-DELETED ALLOCATIONS
    public List<AllocationResponse> getAllAllocations() {

        return allocationRepository.findByIsDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET ALLOCATION BY ID
    public AllocationResponse getAllocationById(Long id) {

        Allocation allocation = getActiveAllocation(id);

        return mapToResponse(allocation);
    }

    // GET ALLOCATIONS BY EMPLOYEE
    public List<AllocationResponse> getEmployeeAllocations(Long employeeId) {

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Employee not found");
        }

        return allocationRepository
                .findByEmployeeIdAndIsDeletedFalse(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET ALLOCATIONS BY PROJECT
    public List<AllocationResponse> getProjectAllocations(Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found");
        }

        return allocationRepository
                .findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // UPDATE ALLOCATION
    @Transactional
    public AllocationResponse updateAllocation(
            Long id,
            AllocationRequest request) {

        Allocation allocation = getActiveAllocation(id);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found"));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));

        validateRequest(request, employee, project);
        validateAvailability(employee, request);
        validateWeeklyCapacity(employee, request, allocation.getId());

        allocation.setEmployee(employee);
        allocation.setProject(project);
        allocation.setStartDate(request.getStartDate());
        allocation.setEndDate(request.getEndDate());
        allocation.setAllocatedHoursPerWeek(
                request.getAllocatedHoursPerWeek());

        Allocation saved = allocationRepository.save(allocation);

        saveAudit(saved, "UPDATED", "Allocation updated");

        return mapToResponse(saved);
    }

    // UPDATE ALLOCATION STATUS
    @Transactional
    public AllocationResponse updateStatus(
            Long id,
            AllocationStatus newStatus) {

        Allocation allocation = getActiveAllocation(id);

        if (newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status is required");
        }

        if (allocation.getStatus() == AllocationStatus.CANCELLED
                || allocation.getStatus() == AllocationStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A completed or cancelled allocation cannot be changed");
        }

        allocation.setStatus(newStatus);

        Allocation saved = allocationRepository.save(allocation);

        saveAudit(saved, "STATUS_CHANGED",
                "Allocation status changed to " + newStatus);

        return mapToResponse(saved);
    }

    // CREATE MULTIPLE PROPOSED ALLOCATIONS
    @Transactional
    public List<AllocationResponse> createAllocations(
            List<AllocationRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one allocation is required"
            );
        }

        // Prevent duplicate employee-project pairs in the same batch
        long uniquePairs = requests.stream()
                .map(request ->
                        request.getEmployeeId() + "-" + request.getProjectId())
                .distinct()
                .count();

        if (uniquePairs != requests.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate employee-project allocations are not allowed"
            );
        }

        List<AllocationResponse> responses = new java.util.ArrayList<>();

        for (AllocationRequest request : requests) {
            responses.add(createAllocation(request));
        }

        return responses;
    }

    // SOFT DELETE ALLOCATION
    @Transactional
    public void softDeleteAllocation(Long id) {

        Allocation allocation = getActiveAllocation(id);

        allocation.setIsDeleted(true);
        allocation.setDeletedAt(LocalDateTime.now());
        allocation.setStatus(AllocationStatus.CANCELLED);

        allocationRepository.save(allocation);

        saveAudit(savedAllocation(allocation), "SOFT_DELETED",
                "Allocation soft-deleted");
    }

    // VALIDATE EMPLOYEE, PROJECT AND REQUEST DETAILS
    private void validateRequest(
            AllocationRequest request,
            Employee employee,
            Project project) {

        if (!employee.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Employee is inactive");
        }

        if (request.getStartDate() == null
                || request.getEndDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Allocation dates are required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date");
        }

        if (request.getAllocatedHoursPerWeek() == null
                || request.getAllocatedHoursPerWeek()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Allocated hours must be greater than zero");
        }

        if (project.getStartDate() == null || project.getEndDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Project dates are not configured");
        }

        if (request.getStartDate().isBefore(project.getStartDate())
                || request.getEndDate().isAfter(project.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Allocation dates must be within project dates");
        }

        if (project.getStatus() != ProjectStatus.ACTIVE
                && project.getStatus() != ProjectStatus.PLANNED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Project is not eligible for allocation");
        }

        if (request.getAllocatedHoursPerWeek().compareTo(
                employee.getWeeklyCapacityHours()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Allocation exceeds employee weekly capacity");
        }
    }

    // VALIDATE AVAILABILITY FOR EVERY PARTIAL CALENDAR WEEK
    private void validateAvailability(
            Employee employee,
            AllocationRequest request) {

        List<Availability> records =
                availabilityRepository
                        .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsDeletedFalse(
                                employee.getId(),
                                request.getEndDate(),
                                request.getStartDate()
                        );

        LocalDate weekStart = request.getStartDate();

        while (!weekStart.isAfter(request.getEndDate())) {

            LocalDate weekEnd = weekStart.plusDays(
                    7 - weekStart.getDayOfWeek().getValue());

            if (weekEnd.isAfter(request.getEndDate())) {
                weekEnd = request.getEndDate();
            }

            final LocalDate segmentStart = weekStart;
            final LocalDate segmentEnd = weekEnd;

            boolean covered = records.stream().anyMatch(a ->
                    !a.getStartDate().isAfter(segmentStart)
                            && !a.getEndDate().isBefore(segmentEnd)
                            && a.getAvailableHoursPerWeek().compareTo(
                            request.getAllocatedHoursPerWeek()) >= 0
            );

            if (!covered) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Availability is missing or insufficient for the period "
                                + segmentStart + " to " + segmentEnd);
            }

            weekStart = weekEnd.plusDays(1);
        }
    }

    // VALIDATE WEEKLY CAPACITY, EXCLUDING CURRENT ALLOCATION ON UPDATE
    private void validateWeeklyCapacity(
            Employee employee,
            AllocationRequest request,
            Long excludeAllocationId) {

        List<Allocation> existing =
                allocationRepository
                        .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusInAndIsDeletedFalse(
                                employee.getId(),
                                request.getEndDate(),
                                request.getStartDate(),
                                List.of(
                                        AllocationStatus.PROPOSED,
                                        AllocationStatus.APPROVED,
                                        AllocationStatus.ACTIVE
                                )
                        );

        LocalDate weekStart = request.getStartDate();

        while (!weekStart.isAfter(request.getEndDate())) {

            LocalDate weekEnd = weekStart.plusDays(
                    7 - weekStart.getDayOfWeek().getValue());

            if (weekEnd.isAfter(request.getEndDate())) {
                weekEnd = request.getEndDate();
            }

            final LocalDate segmentStart = weekStart;
            final LocalDate segmentEnd = weekEnd;

            BigDecimal committed = existing.stream()
                    .filter(a -> excludeAllocationId == null
                            || !a.getId().equals(excludeAllocationId))
                    .filter(a ->
                            !a.getStartDate().isAfter(segmentEnd)
                                    && !a.getEndDate().isBefore(segmentStart))
                    .map(Allocation::getAllocatedHoursPerWeek)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal total = committed.add(
                    request.getAllocatedHoursPerWeek());

            if (total.compareTo(employee.getWeeklyCapacityHours()) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Weekly capacity exceeded for period "
                                + segmentStart + " to " + segmentEnd
                                + ". Available capacity: "
                                + employee.getWeeklyCapacityHours()
                                + ", requested total: " + total);
            }

            weekStart = weekEnd.plusDays(1);
        }
    }

    // FIND ONLY NON-DELETED ALLOCATIONS
    private Allocation getActiveAllocation(Long id) {

        return allocationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Allocation not found"));
    }

    private void saveAudit(
            Allocation allocation,
            String action,
            String reason) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        AppUser actor = null;

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            actor = appUserRepository.findByEmail(authentication.getName())
                    .orElse(null);
        }

        // Python integration and unauthenticated system operations
        // use the existing system account.
        if (actor == null) {
            actor = appUserRepository
                    .findByEmail("allocation-engine@system.local")
                    .orElseThrow(() -> new RuntimeException(
                            "Integration user not found"
                    ));
        }

        AllocationAuditLog log = new AllocationAuditLog();

        log.setAllocation(allocation);
        log.setChangedBy(actor);
        log.setAction(action);
        log.setReason(reason);
        log.setChangedAt(LocalDateTime.now());

        auditRepository.save(log);
    }

    // MAP ENTITY TO RESPONSE DTO
    private AllocationResponse mapToResponse(Allocation allocation) {

        AllocationResponse response = new AllocationResponse();

        response.setId(allocation.getId());

        response.setEmployeeId(allocation.getEmployee().getId());
        response.setEmployeeName(allocation.getEmployee().getName());

        response.setProjectId(allocation.getProject().getId());
        response.setProjectName(allocation.getProject().getProjectName());

        response.setStartDate(allocation.getStartDate());
        response.setEndDate(allocation.getEndDate());

        response.setAllocatedHoursPerWeek(
                allocation.getAllocatedHoursPerWeek());

        response.setStatus(allocation.getStatus());

        return response;
    }

    // Return allocation for audit call
    private Allocation savedAllocation(Allocation allocation) {
        return allocation;
    }

    @Transactional
    public AllocationResponse approveAllocation(Long id) {

        Allocation allocation = getActiveAllocation(id);

        if (allocation.getStatus() != AllocationStatus.PROPOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only proposed allocations can be approved"
            );
        }

        allocation.setStatus(AllocationStatus.ACTIVE);

        Allocation saved = allocationRepository.save(allocation);

        saveAudit(saved, "APPROVED",
                "Allocation approved by manager");

        return mapToResponse(saved);
    }

    @Transactional
    public AllocationResponse rejectAllocation(Long id, String reason) {

        Allocation allocation = getActiveAllocation(id);

        if (allocation.getStatus() != AllocationStatus.PROPOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only proposed allocations can be rejected"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rejection reason is required"
            );
        }

        allocation.setStatus(AllocationStatus.REJECTED);

        Allocation saved = allocationRepository.save(allocation);

        saveAudit(saved, "REJECTED", reason.trim());

        return mapToResponse(saved);
    }

    public List<AllocationResponse> getProposedAllocations() {

        return allocationRepository
                .findByStatusAndIsDeletedFalse(AllocationStatus.PROPOSED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}
