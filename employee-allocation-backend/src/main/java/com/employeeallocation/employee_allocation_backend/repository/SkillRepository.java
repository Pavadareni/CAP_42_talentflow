package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findBySkillNameIgnoreCase(String skillName);

    List<Skill> findByCategoryIgnoreCase(String category);

    boolean existsBySkillNameIgnoreCase(String skillName);
}