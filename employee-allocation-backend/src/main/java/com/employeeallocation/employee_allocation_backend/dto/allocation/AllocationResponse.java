package com.employeeallocation.employee_allocation_backend.dto.allocation;

import com.employeeallocation.employee_allocation_backend.enums.AllocationStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
public class AllocationResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long projectId;
    private String projectName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal allocatedHoursPerWeek;
    private AllocationStatus status;

}