
package com.employeeallocation.employee_allocation_backend.dto.availability;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AvailabilityResponse {

    private Long id;

    private Long employeeId;

    private String employeeCode;

    private String employeeName;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal availableHoursPerWeek;

    private BigDecimal weeklyCapacityHours;
}
