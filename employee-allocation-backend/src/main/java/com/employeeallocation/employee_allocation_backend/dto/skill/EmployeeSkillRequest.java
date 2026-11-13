
package com.employeeallocation.employee_allocation_backend.dto.skill;

import com.employeeallocation.employee_allocation_backend.enums.ProficiencyLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeSkillRequest {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    @NotNull(message = "Proficiency level is required")
    private ProficiencyLevel proficiencyLevel;

    @PastOrPresent(message = "Verification date cannot be in the future")
    private LocalDate lastVerifiedAt;

    private String evidenceSource;
}
