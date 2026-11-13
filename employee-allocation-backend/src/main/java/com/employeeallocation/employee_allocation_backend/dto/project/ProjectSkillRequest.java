
package com.employeeallocation.employee_allocation_backend.dto.project;

import com.employeeallocation.employee_allocation_backend.enums.ProficiencyLevel;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectSkillRequest {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    @NotNull(message = "Required proficiency is required")
    private ProficiencyLevel requiredLevel;

    @NotNull(message = "Required headcount is required")
    @Min(value = 1, message = "Headcount must be at least 1")
    private Integer requiredHeadcount;

    private Boolean mandatory = true;
}
