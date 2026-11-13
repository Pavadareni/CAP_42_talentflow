
package com.employeeallocation.employee_allocation_backend.dto.employee;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EmployeeResponse {

    private Long id;

    private String employeeCode;

    private String name;

    private String email;

    private String department;

    private BigDecimal weeklyCapacityHours;

    private boolean active;
}