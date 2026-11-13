
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.dto.project.ProjectRequest;
import com.employeeallocation.employee_allocation_backend.dto.project.ProjectResponse;
import com.employeeallocation.employee_allocation_backend.entity.Project;
import com.employeeallocation.employee_allocation_backend.repository.ProjectRepository;
import com.employeeallocation.employee_allocation_backend.enums.ProjectStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {

        validateDates(request.getStartDate(), request.getEndDate());

        if (projectRepository.existsByProjectCode(request.getProjectCode().trim())) {
            throw new IllegalArgumentException(
                    "Project code already exists");
        }

        Project project = new Project();

        project.setProjectCode(request.getProjectCode().trim());
        project.setProjectName(request.getProjectName().trim());
        project.setDescription(request.getDescription());
        project.setPriority(request.getPriority());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(request.getStatus());

        return mapToResponse(projectRepository.save(project));
    }


    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAllByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }





    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Project not found: " + id));

        return mapToResponse(project);
    }



    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findAllByActiveTrue()
                .stream()
                .filter(project -> project.getStatus() == status)
                .map(this::mapToResponse)
                .toList();
    }


    @Transactional
    public ProjectResponse updateProject(
            Long id, ProjectRequest request) {

        validateDates(request.getStartDate(), request.getEndDate());

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found: " + id));

        projectRepository.findByProjectCodeIgnoreCase(
                        request.getProjectCode().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Project code already exists");
                });

        project.setProjectCode(request.getProjectCode().trim());
        project.setProjectName(request.getProjectName().trim());
        project.setDescription(request.getDescription());
        project.setPriority(request.getPriority());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(request.getStatus());

        return mapToResponse(projectRepository.save(project));
    }


    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Project not found: " + id));

        if (project.getAllocations() != null &&
                !project.getAllocations().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot deactivate a project with existing allocations");
        }

        project.setActive(false);
        projectRepository.save(project);
    }


    private void validateDates(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {

        if (startDate != null && endDate != null
                && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "Project end date cannot be before start date");
        }
    }

    private ProjectResponse mapToResponse(Project project) {

        ProjectResponse response = new ProjectResponse();

        response.setId(project.getId());
        response.setProjectCode(project.getProjectCode());
        response.setProjectName(project.getProjectName());
        response.setDescription(project.getDescription());
        response.setPriority(project.getPriority());
        response.setStartDate(project.getStartDate());
        response.setEndDate(project.getEndDate());
        response.setStatus(project.getStatus());

        return response;
    }
}
