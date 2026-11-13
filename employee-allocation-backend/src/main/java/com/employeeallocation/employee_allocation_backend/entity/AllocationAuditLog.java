
package com.employeeallocation.employee_allocation_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "allocation_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AllocationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allocation_id", nullable = false)
    private Allocation allocation;

    // Temporarily nullable until authentication is integrated
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "changed_by", nullable = true)
    private AppUser changedBy;

    @Column(nullable = false)
    private String action;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
