
package com.employeeallocation.employee_allocation_backend.dto.project;

import com.employeeallocation.employee_allocation_backend.enums.ProjectStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProjectResponse {

    private Long id;

    private String projectCode;

    private String projectName;

    private String description;

    private Integer priority;

    private LocalDate startDate;

    private LocalDate endDate;

    private ProjectStatus status;

    private Boolean active;
}
