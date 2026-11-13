
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.dto.skill.SkillRequest;
import com.employeeallocation.employee_allocation_backend.dto.skill.SkillResponse;
import com.employeeallocation.employee_allocation_backend.entity.Skill;
import com.employeeallocation.employee_allocation_backend.repository.SkillRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    @Transactional
    public SkillResponse createSkill(SkillRequest request) {

        if (skillRepository.existsBySkillNameIgnoreCase(
                request.getSkillName().trim())) {
            throw new IllegalArgumentException("Skill already exists");
        }

        Skill skill = new Skill();

        skill.setSkillName(request.getSkillName().trim());
        skill.setCategory(request.getCategory().trim());
        skill.setDescription(request.getDescription());

        return mapToResponse(skillRepository.save(skill));
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {

        return skillRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkillById(Long id) {

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Skill not found: " + id));

        return mapToResponse(skill);
    }

    @Transactional
    public SkillResponse updateSkill(Long id, SkillRequest request) {

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Skill not found: " + id));

        skillRepository.findBySkillNameIgnoreCase(
                        request.getSkillName().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Another skill already uses this name");
                });

        skill.setSkillName(request.getSkillName().trim());
        skill.setCategory(request.getCategory().trim());
        skill.setDescription(request.getDescription());

        return mapToResponse(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(Long id) {

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Skill not found: " + id));

        if (skillRepository.existsById(id)) {
            skillRepository.delete(skill);
        }
    }

    private SkillResponse mapToResponse(Skill skill) {

        SkillResponse response = new SkillResponse();

        response.setId(skill.getId());
        response.setSkillName(skill.getSkillName());
        response.setCategory(skill.getCategory());
        response.setDescription(skill.getDescription());

        return response;
    }
}
