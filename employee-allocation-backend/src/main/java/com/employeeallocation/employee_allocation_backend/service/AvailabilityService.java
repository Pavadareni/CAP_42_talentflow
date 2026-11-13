
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.dto.availability.AvailabilityRequest;
import com.employeeallocation.employee_allocation_backend.dto.availability.AvailabilityResponse;
import com.employeeallocation.employee_allocation_backend.entity.Allocation;
import com.employeeallocation.employee_allocation_backend.entity.Availability;
import com.employeeallocation.employee_allocation_backend.entity.Employee;
import com.employeeallocation.employee_allocation_backend.enums.AllocationStatus;
import com.employeeallocation.employee_allocation_backend.repository.AllocationRepository;
import com.employeeallocation.employee_allocation_backend.repository.AvailabilityRepository;
import com.employeeallocation.employee_allocation_backend.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final EmployeeRepository employeeRepository;
    private final AllocationRepository allocationRepository;

    private static final List<AllocationStatus> COMMITTED_STATUSES =
            List.of(
                    AllocationStatus.PROPOSED,
                    AllocationStatus.APPROVED,
                    AllocationStatus.ACTIVE
            );

    // CREATE AVAILABILITY

    @Transactional
    public AvailabilityResponse createAvailability(
            Long employeeId,
            AvailabilityRequest request) {

        Employee employee = getEmployee(employeeId);

        validateEmployee(employee);
        validateRequest(request, employee);

        checkOverlappingAvailability(
                employeeId,
                request.getStartDate(),
                request.getEndDate(),
                null
        );

        validateExistingAllocations(
                employeeId,
                request.getStartDate(),
                request.getEndDate(),
                request.getAvailableHoursPerWeek()
        );

        Availability availability = new Availability();

        availability.setEmployee(employee);
        availability.setStartDate(request.getStartDate());
        availability.setEndDate(request.getEndDate());
        availability.setAvailableHoursPerWeek(
                request.getAvailableHoursPerWeek()
        );

        return mapToResponse(
                availabilityRepository.save(availability)
        );
    }

    // GET ALL AVAILABILITY FOR EMPLOYEE

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getEmployeeAvailability(
            Long employeeId) {

        getEmployee(employeeId);

        return availabilityRepository
                .findByEmployeeIdOrderByStartDateAsc(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET AVAILABILITY BY ID

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailabilityById(Long id) {

        Availability availability = getAvailability(id);

        return mapToResponse(availability);
    }

    // UPDATE AVAILABILITY

    @Transactional
    public AvailabilityResponse updateAvailability(
            Long id,
            AvailabilityRequest request) {

        Availability availability = getAvailability(id);

        Employee employee = availability.getEmployee();

        validateEmployee(employee);
        validateRequest(request, employee);

        checkOverlappingAvailability(
                employee.getId(),
                request.getStartDate(),
                request.getEndDate(),
                id
        );

        validateExistingAllocations(
                employee.getId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getAvailableHoursPerWeek()
        );

        availability.setStartDate(request.getStartDate());
        availability.setEndDate(request.getEndDate());
        availability.setAvailableHoursPerWeek(
                request.getAvailableHoursPerWeek()
        );

        return mapToResponse(
                availabilityRepository.save(availability)
        );
    }

    // DELETE AVAILABILITY

    @Transactional
    public void deleteAvailability(Long id) {

        Availability availability = getAvailability(id);

        availabilityRepository.delete(availability);
    }

    // VALIDATE EMPLOYEE

    private Employee getEmployee(Long employeeId) {

        return employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found: " + employeeId
                        )
                );
    }

    private void validateEmployee(Employee employee) {

        if (!employee.isActive()) {
            throw new IllegalStateException(
                    "Inactive employees cannot have availability updated"
            );
        }
    }

    // VALIDATE REQUEST

    private void validateRequest(
            AvailabilityRequest request,
            Employee employee) {

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        if (request.getAvailableHoursPerWeek().compareTo(
                employee.getWeeklyCapacityHours()) > 0) {

            throw new IllegalArgumentException(
                    "Available hours cannot exceed employee weekly capacity"
            );
        }
    }

    // CHECK OVERLAPPING AVAILABILITY

    private void checkOverlappingAvailability(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Long excludeId) {

        List<Availability> overlapping =
                availabilityRepository
                        .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                employeeId,
                                endDate,
                                startDate
                        );

        boolean conflict = overlapping.stream()
                .anyMatch(a -> !a.getId().equals(excludeId));

        if (conflict) {
            throw new IllegalStateException(
                    "Availability period overlaps with an existing record"
            );
        }
    }

    // CHECK EXISTING PROJECT ALLOCATIONS WEEK BY WEEK

    private void validateExistingAllocations(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal newAvailableHours) {

        LocalDate weekStart = startDate;

        while (!weekStart.isAfter(endDate)) {

            LocalDate weekEnd = weekStart.plusDays(6);

            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            List<Allocation> overlappingAllocations =
                    allocationRepository
                            .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusInAndIsDeletedFalse(
                                    employeeId,
                                    weekEnd,
                                    weekStart,
                                    COMMITTED_STATUSES
                            );

            BigDecimal committedHours = overlappingAllocations
                    .stream()
                    .map(Allocation::getAllocatedHoursPerWeek)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (committedHours.compareTo(newAvailableHours) > 0) {

                throw new IllegalStateException(
                        "Cannot set availability to "
                                + newAvailableHours
                                + " hours. Existing allocations require "
                                + committedHours
                                + " hours during "
                                + weekStart
                                + " to "
                                + weekEnd
                );
            }

            weekStart = weekStart.plusWeeks(1);
        }
    }

    // MAP ENTITY TO RESPONSE

    private AvailabilityResponse mapToResponse(
            Availability availability) {

        AvailabilityResponse response = new AvailabilityResponse();

        response.setId(availability.getId());

        response.setEmployeeId(
                availability.getEmployee().getId()
        );

        response.setEmployeeCode(
                availability.getEmployee().getEmployeeCode()
        );

        response.setEmployeeName(
                availability.getEmployee().getName()
        );

        response.setStartDate(availability.getStartDate());

        response.setEndDate(availability.getEndDate());

        response.setAvailableHoursPerWeek(
                availability.getAvailableHoursPerWeek()
        );

        response.setWeeklyCapacityHours(
                availability.getEmployee().getWeeklyCapacityHours()
        );

        return response;
    }

    private Availability getAvailability(Long id) {

        return availabilityRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Availability record not found: " + id
                        )
                );
    }
}
