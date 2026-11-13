
package com.employeeallocation.employee_allocation_backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvisionEmployeeAccountRequest(

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100)
        String password

) {}
