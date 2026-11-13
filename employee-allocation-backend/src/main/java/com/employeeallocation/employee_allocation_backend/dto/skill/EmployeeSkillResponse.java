
package com.employeeallocation.employee_allocation_backend.dto.skill;

import com.employeeallocation.employee_allocation_backend.enums.ProficiencyLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeSkillResponse {

    private Long id;

    private Long employeeId;

    private String employeeName;

    private Long skillId;

    private String skillName;

    private String category;

    private ProficiencyLevel proficiencyLevel;

    private LocalDate lastVerifiedAt;

    private String evidenceSource;
}
