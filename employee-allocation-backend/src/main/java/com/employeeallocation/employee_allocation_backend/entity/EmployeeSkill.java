
package com.employeeallocation.employee_allocation_backend.entity;

import com.employeeallocation.employee_allocation_backend.enums.ProficiencyLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_skills",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"employee_id", "skill_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EmployeeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProficiencyLevel proficiencyLevel;

    private LocalDate lastVerifiedAt;

    private String evidenceSource;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
