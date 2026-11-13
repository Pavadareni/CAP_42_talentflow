package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.project.ProjectSkillRequest;
import com.employeeallocation.employee_allocation_backend.dto.project.ProjectSkillResponse;
import com.employeeallocation.employee_allocation_backend.service.ProjectSkillService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/skills")
@RequiredArgsConstructor
public class ProjectSkillController {

    private final ProjectSkillService projectSkillService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProjectSkillResponse> addProjectSkill(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectSkillRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectSkillService.addProjectSkill(projectId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SYSTEM')")
    public ResponseEntity<List<ProjectSkillResponse>> getProjectSkills(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                projectSkillService.getProjectSkills(projectId));
    }

    @PutMapping("/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProjectSkillResponse> updateProjectSkill(
            @PathVariable Long projectId,
            @PathVariable Long skillId,
            @Valid @RequestBody ProjectSkillRequest request) {

        return ResponseEntity.ok(
                projectSkillService.updateProjectSkill(
                        projectId, skillId, request));
    }

    @DeleteMapping("/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> removeProjectSkill(
            @PathVariable Long projectId,
            @PathVariable Long skillId) {

        projectSkillService.removeProjectSkill(projectId, skillId);

        return ResponseEntity.noContent().build();
    }
}