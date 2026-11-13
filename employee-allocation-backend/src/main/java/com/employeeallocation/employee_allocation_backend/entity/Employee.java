package com.employeeallocation.employee_allocation_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String department;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal weeklyCapacityHours;

    @Column(nullable = false)
    private boolean active = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private AppUser user;

    @OneToMany(mappedBy = "employee")
    private List<EmployeeSkill> employeeSkills;

    @OneToMany(mappedBy = "employee")
    private List<Availability> availabilities;

    @OneToMany(mappedBy = "employee")
    private List<Allocation> allocations;


}