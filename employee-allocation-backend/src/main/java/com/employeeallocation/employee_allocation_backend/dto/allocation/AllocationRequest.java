package com.employeeallocation.employee_allocation_backend.dto.allocation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
public class AllocationRequest {

    @NotNull
    @Positive
    private Long employeeId;

    @NotNull
    @Positive
    private Long projectId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal allocatedHoursPerWeek;

}