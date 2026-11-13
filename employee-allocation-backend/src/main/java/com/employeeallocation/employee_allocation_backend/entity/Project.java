package com.employeeallocation.employee_allocation_backend.entity;

import com.employeeallocation.employee_allocation_backend.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String projectCode;

    @Column(nullable = false)
    private String projectName;

    private String description;

    private Integer priority;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @OneToMany(mappedBy = "project")
    private List<ProjectSkill> projectSkills;

    @OneToMany(mappedBy = "project")
    private List<Allocation> allocations;

    @Column(nullable = false)
    private Boolean active = true;

}