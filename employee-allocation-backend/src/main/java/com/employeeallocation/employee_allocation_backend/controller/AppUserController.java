
package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.user.ProvisionEmployeeAccountRequest;
import com.employeeallocation.employee_allocation_backend.entity.AppUser;
import com.employeeallocation.employee_allocation_backend.service.AppUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @PostMapping("/employees/{employeeId}/account")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createEmployeeAccount(
            @PathVariable Long employeeId,
            @Valid @RequestBody ProvisionEmployeeAccountRequest request) {

        AppUser user = appUserService.createEmployeeAccount(
                employeeId, request);

        return Map.of(
                "message", "Employee account created successfully",
                "userId", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "enabled", user.isEnabled()
        );
    }
}
