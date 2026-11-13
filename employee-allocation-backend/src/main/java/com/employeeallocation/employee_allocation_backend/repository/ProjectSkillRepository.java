package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.ProjectSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectSkillRepository
        extends JpaRepository<ProjectSkill, Long> {

    List<ProjectSkill> findByProjectId(Long projectId);

    List<ProjectSkill> findBySkillId(Long skillId);

    boolean existsByProjectIdAndSkillId(
            Long projectId, Long skillId
    );

    Optional<ProjectSkill> findByProjectIdAndSkillId(
            Long projectId,
            Long skillId
    );
    List<ProjectSkill> findByProjectIdAndIsDeletedFalse(Long projectId);
    List<ProjectSkill> findBySkillIdAndIsDeletedFalse(Long skillId);
    Optional<ProjectSkill> findByProjectIdAndSkillIdAndIsDeletedFalse(
            Long projectId,
            Long skillId);

}