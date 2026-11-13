
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.dto.project.ProjectSkillRequest;
import com.employeeallocation.employee_allocation_backend.dto.project.ProjectSkillResponse;
import com.employeeallocation.employee_allocation_backend.entity.Project;
import com.employeeallocation.employee_allocation_backend.entity.ProjectSkill;
import com.employeeallocation.employee_allocation_backend.entity.Skill;
import com.employeeallocation.employee_allocation_backend.repository.ProjectRepository;
import com.employeeallocation.employee_allocation_backend.repository.ProjectSkillRepository;
import com.employeeallocation.employee_allocation_backend.repository.SkillRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectSkillService {

    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ProjectSkillRepository projectSkillRepository;

    @Transactional
    public ProjectSkillResponse addProjectSkill(
            Long projectId,
            ProjectSkillRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Project not found"));

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Skill not found"));

        if (projectSkillRepository.existsByProjectIdAndSkillId(
                projectId, request.getSkillId())) {
            throw new IllegalArgumentException(
                    "This skill is already required by the project");
        }

        ProjectSkill projectSkill = new ProjectSkill();

        projectSkill.setProject(project);
        projectSkill.setSkill(skill);
        projectSkill.setRequiredLevel(request.getRequiredLevel());
        projectSkill.setRequiredHeadcount(request.getRequiredHeadcount());
        projectSkill.setMandatory(
                request.getMandatory() == null || request.getMandatory()
        );

        return mapToResponse(projectSkillRepository.save(projectSkill));
    }

    @Transactional(readOnly = true)
    public List<ProjectSkillResponse> getProjectSkills(Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project not found");
        }

        return projectSkillRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ProjectSkillResponse updateProjectSkill(
            Long projectId,
            Long skillId,
            ProjectSkillRequest request) {

        ProjectSkill projectSkill =
                projectSkillRepository
                        .findByProjectIdAndSkillId(projectId, skillId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Project skill requirement not found"));

        if (!skillId.equals(request.getSkillId())) {
            throw new IllegalArgumentException(
                    "Skill ID in request must match URL skill ID");
        }

        projectSkill.setRequiredLevel(request.getRequiredLevel());
        projectSkill.setRequiredHeadcount(request.getRequiredHeadcount());
        projectSkill.setMandatory(
                request.getMandatory() == null || request.getMandatory()
        );

        return mapToResponse(
                projectSkillRepository.save(projectSkill));
    }

    @Transactional
    public void removeProjectSkill(Long projectId, Long skillId) {

        ProjectSkill projectSkill =
                projectSkillRepository
                        .findByProjectIdAndSkillId(projectId, skillId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Project skill requirement not found"));

        projectSkill.setIsDeleted(true);
        projectSkill.setDeletedAt(LocalDateTime.now());

        projectSkillRepository.save(projectSkill);
    }

    private ProjectSkillResponse mapToResponse(
            ProjectSkill projectSkill) {

        ProjectSkillResponse response = new ProjectSkillResponse();

        response.setId(projectSkill.getId());

        response.setProjectId(
                projectSkill.getProject().getId());

        response.setProjectName(
                projectSkill.getProject().getProjectName());

        response.setSkillId(
                projectSkill.getSkill().getId());

        response.setSkillName(
                projectSkill.getSkill().getSkillName());

        response.setCategory(
                projectSkill.getSkill().getCategory());

        response.setRequiredLevel(
                projectSkill.getRequiredLevel());

        response.setRequiredHeadcount(
                projectSkill.getRequiredHeadcount());

        return response;
    }
}
