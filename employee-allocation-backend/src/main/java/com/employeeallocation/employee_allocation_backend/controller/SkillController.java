
package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.skill.SkillRequest;
import com.employeeallocation.employee_allocation_backend.dto.skill.SkillResponse;
import com.employeeallocation.employee_allocation_backend.service.SkillService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(
            @Valid @RequestBody SkillRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skillService.createSkill(request));
    }

    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {

        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(
            @PathVariable Long id) {

        return ResponseEntity.ok(skillService.getSkillById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody SkillRequest request) {

        return ResponseEntity.ok(
                skillService.updateSkill(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(
            @PathVariable Long id) {

        skillService.deleteSkill(id);

        return ResponseEntity.noContent().build();
    }
}
