package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.Project;
import com.employeeallocation.employee_allocation_backend.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByProjectCode(String projectCode);

    boolean existsByProjectCode(String projectCode);

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByPriorityGreaterThanEqual(Integer priority);

    Optional<Project> findByProjectCodeIgnoreCase(String projectCode);

    List<Project> findByStatusOrderByPriorityDesc(
            ProjectStatus status
    );

    List<Project> findAllByActiveTrue();

    Optional<Project> findByIdAndActiveTrue(Long id);

}