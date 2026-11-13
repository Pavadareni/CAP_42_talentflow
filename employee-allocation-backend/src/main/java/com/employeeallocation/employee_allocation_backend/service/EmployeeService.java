
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.dto.employee.EmployeeRequest;
import com.employeeallocation.employee_allocation_backend.dto.employee.EmployeeResponse;
import com.employeeallocation.employee_allocation_backend.entity.Employee;
import com.employeeallocation.employee_allocation_backend.repository.EmployeeRepository;
import com.employeeallocation.employee_allocation_backend.dto.allocation.AllocationResponse;
import com.employeeallocation.employee_allocation_backend.dto.availability.AvailabilityResponse;
import com.employeeallocation.employee_allocation_backend.entity.Availability;
import com.employeeallocation.employee_allocation_backend.repository.AvailabilityRepository;
import com.employeeallocation.employee_allocation_backend.service.AllocationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.employeeallocation.employee_allocation_backend.entity.AppUser;
import com.employeeallocation.employee_allocation_backend.enums.UserRole;
import com.employeeallocation.employee_allocation_backend.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.employeeallocation.employee_allocation_backend.dto.skill.EmployeeSkillResponse;
import com.employeeallocation.employee_allocation_backend.entity.EmployeeSkill;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AllocationService allocationService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {

        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new IllegalArgumentException("Employee code already exists");
        }

        if (employeeRepository.findByEmail(request.getEmail()).isPresent()
                || appUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (request.getRole() != UserRole.EMPLOYEE
                && request.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException(
                    "Only EMPLOYEE or MANAGER accounts can be created here"
            );
        }

        // 1. Create login account
        AppUser user = new AppUser();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEnabled(true);

        AppUser savedUser = appUserRepository.save(user);

        // 2. Create employee profile
        Employee employee = new Employee();
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        if (employee.getUser() != null) {
            employee.getUser().setEmail(request.getEmail());
        }
        employee.setDepartment(request.getDepartment());
        employee.setWeeklyCapacityHours(request.getWeeklyCapacityHours());
        employee.setActive(true);

        // 3. Link employee to login account
        employee.setUser(savedUser);

        Employee savedEmployee = employeeRepository.save(employee);

        return mapToResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {

        return employeeRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found: " + id));

        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found: " + id));

        // 1. Check duplicate employee code
        employeeRepository.findByEmployeeCode(request.getEmployeeCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Employee code already exists");
                });

        // 2. Check duplicate employee email
        employeeRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Employee email already exists");
                });

        // 3. Check duplicate login email
        appUserRepository.findByEmail(request.getEmail())
                .filter(existingUser ->
                        employee.getUser() == null
                                || !existingUser.getId().equals(employee.getUser().getId()))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Login email already exists");
                });

        // 4. Update employee details
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setWeeklyCapacityHours(request.getWeeklyCapacityHours());

        // 5. Keep login email synchronized
        if (employee.getUser() != null) {
            employee.getUser().setEmail(request.getEmail());
        }

        return mapToResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void deactivateEmployee(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found: " + id));

        employee.setActive(false);

        employeeRepository.save(employee);
    }

    private EmployeeResponse mapToResponse(Employee employee) {

        EmployeeResponse response = new EmployeeResponse();

        response.setId(employee.getId());
        response.setEmployeeCode(employee.getEmployeeCode());
        response.setName(employee.getName());
        response.setEmail(employee.getEmail());
        response.setDepartment(employee.getDepartment());
        response.setWeeklyCapacityHours(employee.getWeeklyCapacityHours());
        response.setActive(employee.isActive());

        return response;
    }

    // GET LOGGED-IN EMPLOYEE
    @Transactional(readOnly = true)
    public EmployeeResponse getMyProfile() {

        Employee employee = getLoggedInEmployee();

        return mapToResponse(employee);
    }

    // GET LOGGED-IN EMPLOYEE'S ALLOCATIONS
    @Transactional(readOnly = true)
    public List<AllocationResponse> getMyAllocations() {

        Employee employee = getLoggedInEmployee();

        return allocationService.getEmployeeAllocations(employee.getId());
    }

    // GET LOGGED-IN EMPLOYEE'S AVAILABILITY
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getMyAvailability() {

        Employee employee = getLoggedInEmployee();

        return availabilityRepository
                .findByEmployeeIdAndIsDeletedFalseOrderByStartDateAsc(employee.getId())
                .stream()
                .map(this::mapAvailabilityToResponse)
                .toList();
    }

    // IDENTIFY EMPLOYEE FROM JWT EMAIL
    private Employee getLoggedInEmployee() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return employeeRepository.findByUser_Email(email)
                .filter(Employee::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No active employee profile is linked to this account"
                ));
    }

    // MAP AVAILABILITY ENTITY TO RESPONSE
    private AvailabilityResponse mapAvailabilityToResponse(
            Availability availability) {

        AvailabilityResponse response = new AvailabilityResponse();

        response.setId(availability.getId());
        response.setEmployeeId(availability.getEmployee().getId());
        response.setEmployeeCode(availability.getEmployee().getEmployeeCode());
        response.setEmployeeName(availability.getEmployee().getName());
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

    // GET LOGGED-IN EMPLOYEE'S SKILLS
    @Transactional(readOnly = true)
    public List<EmployeeSkillResponse> getMySkills() {

        Employee employee = getLoggedInEmployee();

        return employee.getEmployeeSkills()
                .stream()
                .map(this::mapEmployeeSkillToResponse)
                .toList();
    }

    // MAP EMPLOYEE SKILL TO RESPONSE DTO
    private EmployeeSkillResponse mapEmployeeSkillToResponse(
            EmployeeSkill employeeSkill) {

        EmployeeSkillResponse response = new EmployeeSkillResponse();

        response.setId(employeeSkill.getId());
        response.setEmployeeId(employeeSkill.getEmployee().getId());
        response.setEmployeeName(employeeSkill.getEmployee().getName());

        response.setSkillId(employeeSkill.getSkill().getId());
        response.setSkillName(employeeSkill.getSkill().getSkillName());
        response.setCategory(employeeSkill.getSkill().getCategory());

        response.setProficiencyLevel(employeeSkill.getProficiencyLevel());
        response.setLastVerifiedAt(employeeSkill.getLastVerifiedAt());
        response.setEvidenceSource(employeeSkill.getEvidenceSource());

        return response;
    }


}