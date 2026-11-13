
package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.project.ProjectRequest;
import com.employeeallocation.employee_allocation_backend.dto.project.ProjectResponse;
import com.employeeallocation.employee_allocation_backend.enums.ProjectStatus;
import com.employeeallocation.employee_allocation_backend.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SYSTEM')")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {

        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SYSTEM')")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id) {

        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SYSTEM')")
    public ResponseEntity<List<ProjectResponse>> getProjectsByStatus(
            @PathVariable ProjectStatus status) {

        return ResponseEntity.ok(
                projectService.getProjectsByStatus(status));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {

        return ResponseEntity.ok(
                projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id) {

        projectService.deleteProject(id);

        return ResponseEntity.noContent().build();
    }
}
