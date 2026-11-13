
package com.employeeallocation.employee_allocation_backend.dto.skill;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillResponse {

    private Long id;

    private String skillName;

    private String category;

    private String description;
}
