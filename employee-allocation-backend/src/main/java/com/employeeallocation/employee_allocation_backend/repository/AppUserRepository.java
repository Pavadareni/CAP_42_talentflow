package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);


}