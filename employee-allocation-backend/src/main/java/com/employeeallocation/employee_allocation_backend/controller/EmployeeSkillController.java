
package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.skill.EmployeeSkillRequest;
import com.employeeallocation.employee_allocation_backend.dto.skill.EmployeeSkillResponse;
import com.employeeallocation.employee_allocation_backend.service.EmployeeSkillService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/employees/{employeeId}/skills")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class EmployeeSkillController {

    private final EmployeeSkillService employeeSkillService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EmployeeSkillResponse> assignSkill(
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeeSkillRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeSkillService.assignSkill(
                        employeeId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SYSTEM')")
    public ResponseEntity<List<EmployeeSkillResponse>> getEmployeeSkills(
            @PathVariable Long employeeId) {

        return ResponseEntity.ok(
                employeeSkillService.getEmployeeSkills(employeeId));
    }

    @PutMapping("/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EmployeeSkillResponse> updateEmployeeSkill(
            @PathVariable Long employeeId,
            @PathVariable Long skillId,
            @Valid @RequestBody EmployeeSkillRequest request) {

        return ResponseEntity.ok(
                employeeSkillService.updateEmployeeSkill(
                        employeeId, skillId, request));
    }

    @DeleteMapping("/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> removeEmployeeSkill(
            @PathVariable Long employeeId,
            @PathVariable Long skillId) {

        employeeSkillService.removeEmployeeSkill(employeeId, skillId);

        return ResponseEntity.noContent().build();
    }
}
