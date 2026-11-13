
package com.employeeallocation.employee_allocation_backend.service;

import com.employeeallocation.employee_allocation_backend.dto.user.ProvisionEmployeeAccountRequest;
import com.employeeallocation.employee_allocation_backend.entity.AppUser;
import com.employeeallocation.employee_allocation_backend.entity.Employee;
import com.employeeallocation.employee_allocation_backend.enums.UserRole;
import com.employeeallocation.employee_allocation_backend.repository.AppUserRepository;
import com.employeeallocation.employee_allocation_backend.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AppUser createEmployeeAccount(
            Long employeeId,
            ProvisionEmployeeAccountRequest request) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found"));

        if (!employee.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot provision an inactive employee");
        }

        if (employee.getUser() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee already has a login account");
        }

        if (appUserRepository.existsByEmail(employee.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already belongs to another user");
        }

        AppUser user = new AppUser();
        user.setEmail(employee.getEmail());
        user.setPasswordHash(
                passwordEncoder.encode(request.password()));
        user.setRole(UserRole.EMPLOYEE);
        user.setEnabled(true);

        AppUser savedUser = appUserRepository.save(user);

        employee.setUser(savedUser);
        employeeRepository.save(employee);

        return savedUser;
    }
}
