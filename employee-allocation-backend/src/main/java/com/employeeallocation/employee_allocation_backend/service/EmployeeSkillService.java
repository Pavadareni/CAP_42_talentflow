
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.dto.skill.EmployeeSkillRequest;
import com.employeeallocation.employee_allocation_backend.dto.skill.EmployeeSkillResponse;
import com.employeeallocation.employee_allocation_backend.entity.Employee;
import com.employeeallocation.employee_allocation_backend.entity.EmployeeSkill;
import com.employeeallocation.employee_allocation_backend.entity.Skill;
import com.employeeallocation.employee_allocation_backend.repository.EmployeeRepository;
import com.employeeallocation.employee_allocation_backend.repository.EmployeeSkillRepository;
import com.employeeallocation.employee_allocation_backend.repository.SkillRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeSkillService {

    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;

    @Transactional
    public EmployeeSkillResponse assignSkill(
            Long employeeId,
            EmployeeSkillRequest request) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found"));

        if (!employee.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot assign skills to an inactive employee");
        }

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Skill not found"));

        EmployeeSkill employeeSkill =
                employeeSkillRepository
                        .findByEmployeeIdAndSkillId(
                                employeeId, request.getSkillId())
                        .orElse(null);

        if (employeeSkill != null && !employeeSkill.isDeleted()) {
            throw new IllegalArgumentException(
                    "Employee already has this skill");
        }

        if (employeeSkill == null) {
            employeeSkill = new EmployeeSkill();
            employeeSkill.setEmployee(employee);
            employeeSkill.setSkill(skill);
        } else {
            // Restore existing record to respect unique constraint.
            employeeSkill.setDeleted(false);
            employeeSkill.setDeletedAt(null);
        }

        employeeSkill.setProficiencyLevel(request.getProficiencyLevel());
        employeeSkill.setLastVerifiedAt(request.getLastVerifiedAt());
        employeeSkill.setEvidenceSource(request.getEvidenceSource());

        return mapToResponse(
                employeeSkillRepository.save(employeeSkill));
    }

    @Transactional(readOnly = true)
    public List<EmployeeSkillResponse> getEmployeeSkills(
            Long employeeId) {

        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found");
        }

        return employeeSkillRepository
                .findByEmployeeIdAndIsDeletedFalse(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public EmployeeSkillResponse updateEmployeeSkill(
            Long employeeId,
            Long skillId,
            EmployeeSkillRequest request) {

        if (!skillId.equals(request.getSkillId())) {
            throw new IllegalArgumentException(
                    "Skill ID in request must match the URL skill ID");
        }

        EmployeeSkill employeeSkill =
                employeeSkillRepository
                        .findByEmployeeIdAndSkillIdAndIsDeletedFalse(
                                employeeId, skillId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Active employee skill mapping not found"));

        employeeSkill.setProficiencyLevel(request.getProficiencyLevel());
        employeeSkill.setLastVerifiedAt(request.getLastVerifiedAt());
        employeeSkill.setEvidenceSource(request.getEvidenceSource());

        return mapToResponse(
                employeeSkillRepository.save(employeeSkill));
    }

    @Transactional
    public void removeEmployeeSkill(Long employeeId, Long skillId) {

        EmployeeSkill employeeSkill =
                employeeSkillRepository
                        .findByEmployeeIdAndSkillIdAndIsDeletedFalse(
                                employeeId, skillId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Active employee skill mapping not found"));

        employeeSkill.setDeleted(true);
        employeeSkill.setDeletedAt(LocalDateTime.now());

        employeeSkillRepository.save(employeeSkill);
    }

    private EmployeeSkillResponse mapToResponse(
            EmployeeSkill employeeSkill) {

        EmployeeSkillResponse response = new EmployeeSkillResponse();

        response.setId(employeeSkill.getId());
        response.setEmployeeId(employeeSkill.getEmployee().getId());
        response.setEmployeeName(employeeSkill.getEmployee().getName());
        response.setSkillId(employeeSkill.getSkill().getId());
        response.setSkillName(employeeSkill.getSkill().getSkillName());
        response.setCategory(employeeSkill.getSkill().getCategory());
        response.setProficiencyLevel(
                employeeSkill.getProficiencyLevel());
        response.setLastVerifiedAt(
                employeeSkill.getLastVerifiedAt());
        response.setEvidenceSource(
                employeeSkill.getEvidenceSource());

        return response;
    }
}
