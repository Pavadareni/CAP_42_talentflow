
package com.employeeallocation.employee_allocation_backend.dto.project;

import com.employeeallocation.employee_allocation_backend.enums.ProjectStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProjectRequest {

    @NotBlank(message = "Project code is required")
    private String projectCode;

    @NotBlank(message = "Project name is required")
    private String projectName;

    private String description;

    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 5, message = "Priority cannot exceed 5")
    private Integer priority;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Project status is required")
    private ProjectStatus status;
}
