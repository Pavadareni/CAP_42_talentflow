
package com.employeeallocation.employee_allocation_backend.dto.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillRequest {

    @NotBlank(message = "Skill name is required")
    @Size(max = 100)
    private String skillName;

    @NotBlank(message = "Skill category is required")
    @Size(max = 100)
    private String category;

    @Size(max = 500)
    private String description;
}
