package com.employeeallocation.employee_allocation_backend.config;

import com.employeeallocation.employee_allocation_backend.entity.AppUser;
import com.employeeallocation.employee_allocation_backend.repository.AppUserRepository;
import com.employeeallocation.employee_allocation_backend.enums.UserRole;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

@Configuration
public class SystemUserInitializer {

    @Bean
    CommandLineRunner createSystemUser(AppUserRepository appUserRepository) {
        return args -> {

            String email = "allocation-engine@system.local";

            // Diagnostic messages
            System.out.println("Initializer started");

            System.out.println("Database contains system user: "
                    + appUserRepository.existsByEmail(email));

            if (appUserRepository.findByEmail(email).isEmpty()) {

                AppUser systemUser = new AppUser();

                systemUser.setEmail(email);
                systemUser.setPasswordHash(
                        new BCryptPasswordEncoder()
                                .encode(UUID.randomUUID().toString())
                );
                systemUser.setRole(UserRole.ADMIN);
                systemUser.setEnabled(false);

                appUserRepository.save(systemUser);

                System.out.println("System user created successfully.");
            }
        };
    }
}