
package com.employeeallocation.employee_allocation_backend.dto.project;

import com.employeeallocation.employee_allocation_backend.enums.ProficiencyLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectSkillResponse {

    private Long id;

    private Long projectId;

    private String projectName;

    private Long skillId;

    private String skillName;

    private String category;

    private ProficiencyLevel requiredLevel;

    private Integer requiredHeadcount;
    private Boolean mandatory = true;
}
