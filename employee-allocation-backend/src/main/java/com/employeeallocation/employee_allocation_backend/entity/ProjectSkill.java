package com.employeeallocation.employee_allocation_backend.entity;

import com.employeeallocation.employee_allocation_backend.enums.ProficiencyLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "project_skills",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"project_id", "skill_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProficiencyLevel requiredLevel;

    @Column(nullable = false)
    private Integer requiredHeadcount;

    @Column(nullable = false)
    private boolean mandatory = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    private LocalDateTime deletedAt;
}