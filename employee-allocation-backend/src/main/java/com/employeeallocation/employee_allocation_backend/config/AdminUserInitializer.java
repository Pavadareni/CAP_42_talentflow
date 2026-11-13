
package com.employeeallocation.employee_allocation_backend.config;

import com.employeeallocation.employee_allocation_backend.entity.AppUser;
import com.employeeallocation.employee_allocation_backend.enums.UserRole;
import com.employeeallocation.employee_allocation_backend.repository.AppUserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AdminUserInitializer {

    @Bean
    CommandLineRunner createAdminUser(
            AppUserRepository appUserRepository) {

        return args -> {

            String email = "admin@gmail.com";

            String adminPassword =
                    System.getenv("APP_ADMIN_PASSWORD");

            if (appUserRepository.existsByEmail(email)) {
                System.out.println("Admin account already exists.");
                return;
            }

            if (adminPassword == null || adminPassword.isBlank()) {
                System.out.println(
                        "APP_ADMIN_PASSWORD is not configured. Admin creation skipped."
                );
                return;
            }

            AppUser admin = new AppUser();

            admin.setEmail(email);
            admin.setPasswordHash(
                    new BCryptPasswordEncoder().encode(adminPassword)
            );
            admin.setRole(UserRole.ADMIN);
            admin.setEnabled(true);

            appUserRepository.save(admin);

            System.out.println(
                    "Interactive admin account created successfully."
            );
        };
    }
}
