
package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.auth.LoginRequest;
import com.employeeallocation.employee_allocation_backend.dto.auth.LoginResponse;
import com.employeeallocation.employee_allocation_backend.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
