
package com.employeeallocation.employee_allocation_backend.dto.availability;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AvailabilityRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Available hours are required")
    @DecimalMin(value = "0.01",
            message = "Available hours must be greater than zero")
    @DecimalMax(value = "168.00",
            message = "Available hours cannot exceed 168")
    private BigDecimal availableHoursPerWeek;
}
