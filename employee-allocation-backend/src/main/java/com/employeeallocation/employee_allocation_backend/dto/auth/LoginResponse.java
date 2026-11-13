
package com.employeeallocation.employee_allocation_backend.dto.auth;

public record LoginResponse(

        String token,

        String tokenType,

        long expiresIn,

        String email,

        String role

) {
}
