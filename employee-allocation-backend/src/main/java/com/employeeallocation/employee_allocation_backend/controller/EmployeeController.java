
package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.allocation.AllocationResponse;
import com.employeeallocation.employee_allocation_backend.dto.availability.AvailabilityResponse;
import com.employeeallocation.employee_allocation_backend.dto.employee.EmployeeRequest;
import com.employeeallocation.employee_allocation_backend.dto.employee.EmployeeResponse;
import com.employeeallocation.employee_allocation_backend.service.EmployeeService;
import com.employeeallocation.employee_allocation_backend.dto.skill.EmployeeSkillResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SYSTEM')")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {

        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SYSTEM')")
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                employeeService.getEmployeeById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {


        return ResponseEntity.ok(
                employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deactivateEmployee(
            @PathVariable Long id) {

        employeeService.deactivateEmployee(id);

        return ResponseEntity.ok(
                "Employee deactivated successfully");
    }
    // EMPLOYEE SELF-SERVICE PROFILE
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<EmployeeResponse> getMyProfile() {

        return ResponseEntity.ok(employeeService.getMyProfile());
    }

    // EMPLOYEE SELF-SERVICE ALLOCATIONS
    @GetMapping("/me/allocations")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<AllocationResponse>> getMyAllocations() {

        return ResponseEntity.ok(employeeService.getMyAllocations());
    }

    // EMPLOYEE SELF-SERVICE AVAILABILITY
    @GetMapping("/me/availability")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<AvailabilityResponse>> getMyAvailability() {

        return ResponseEntity.ok(employeeService.getMyAvailability());
    }

    // EMPLOYEE SELF-SERVICE SKILLS
    @GetMapping("/me/skills")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<EmployeeSkillResponse>> getMySkills() {

        return ResponseEntity.ok(employeeService.getMySkills());
    }
}
