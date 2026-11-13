package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.AllocationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AllocationAuditLogRepository
        extends JpaRepository<AllocationAuditLog, Long> {

    List<AllocationAuditLog> findByAllocationId(Long allocationId);

    List<AllocationAuditLog> findByChangedById(Long userId);
}