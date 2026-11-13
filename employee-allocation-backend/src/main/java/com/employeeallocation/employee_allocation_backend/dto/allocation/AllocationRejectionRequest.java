package com.employeeallocation.employee_allocation_backend.dto.allocation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AllocationRejectionRequest(

        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason

) {}