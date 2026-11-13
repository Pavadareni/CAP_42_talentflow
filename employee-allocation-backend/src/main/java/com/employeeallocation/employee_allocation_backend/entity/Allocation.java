package com.employeeallocation.employee_allocation_backend.entity;

import com.employeeallocation.employee_allocation_backend.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "allocations",
        indexes = {
                @Index(name = "idx_allocation_employee_dates",
                        columnList = "employee_id,start_date,end_date"),
                @Index(name = "idx_allocation_project",
                        columnList = "project_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal allocatedHoursPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    private LocalDateTime deletedAt;

    private Long deletedBy;

}