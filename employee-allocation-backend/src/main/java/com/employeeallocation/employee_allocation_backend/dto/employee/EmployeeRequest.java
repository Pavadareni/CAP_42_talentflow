package com.employeeallocation.employee_allocation_backend.dto.employee;

import com.employeeallocation.employee_allocation_backend.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EmployeeRequest {

    @NotBlank(message = "Employee code is required")
    private String employeeCode;

    @NotBlank(message = "Employee name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    private String department;

    @NotNull(message = "Weekly capacity is required")
    @DecimalMin(value = "0.01", message = "Capacity must be greater than zero")
    @DecimalMax(value = "168.00", message = "Capacity cannot exceed 168 hours")
    private BigDecimal weeklyCapacityHours;

    @NotNull(message = "Account role is required")
    private UserRole role;

    @NotBlank(message = "Initial password is required")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;
}