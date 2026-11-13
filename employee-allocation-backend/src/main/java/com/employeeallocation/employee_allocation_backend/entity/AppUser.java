package com.employeeallocation.employee_allocation_backend.entity;

import com.employeeallocation.employee_allocation_backend.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Employee employee;
}