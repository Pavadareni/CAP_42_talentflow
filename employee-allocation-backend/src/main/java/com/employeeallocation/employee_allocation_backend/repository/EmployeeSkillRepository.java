
package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.EmployeeSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeSkillRepository
        extends JpaRepository<EmployeeSkill, Long> {

    List<EmployeeSkill> findByEmployeeIdAndIsDeletedFalse(Long employeeId);

    List<EmployeeSkill> findBySkillIdAndIsDeletedFalse(Long skillId);

    Optional<EmployeeSkill> findByEmployeeIdAndSkillId(
            Long employeeId,
            Long skillId
    );

    Optional<EmployeeSkill> findByEmployeeIdAndSkillIdAndIsDeletedFalse(
            Long employeeId,
            Long skillId
    );

    boolean existsByEmployeeIdAndSkillIdAndIsDeletedFalse(
            Long employeeId,
            Long skillId
    );
}
