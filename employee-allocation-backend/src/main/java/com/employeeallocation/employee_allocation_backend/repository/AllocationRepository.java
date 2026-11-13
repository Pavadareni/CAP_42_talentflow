package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.Allocation;
import com.employeeallocation.employee_allocation_backend.enums.AllocationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AllocationRepository
        extends JpaRepository<Allocation, Long> {

    // Get only non-deleted allocations
    List<Allocation> findByIsDeletedFalse();

    Optional<Allocation> findByIdAndIsDeletedFalse(Long id);

    List<Allocation> findByEmployeeIdAndIsDeletedFalse(Long employeeId);

    List<Allocation> findByProjectIdAndIsDeletedFalse(Long projectId);

    List<Allocation> findByStatusAndIsDeletedFalse(AllocationStatus status);

    // Total allocated hours, excluding deleted allocations
    @Query("""
        SELECT COALESCE(SUM(a.allocatedHoursPerWeek), 0)
        FROM Allocation a
        WHERE a.employee.id = :employeeId
        AND a.isDeleted = false
        AND a.status IN :statuses
        AND a.startDate <= :endDate
        AND a.endDate >= :startDate
    """)
    BigDecimal getTotalAllocatedHours(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<AllocationStatus> statuses
    );

    // Find overlapping non-deleted allocations
    @Query("""
        SELECT a FROM Allocation a
        WHERE a.employee.id = :employeeId
        AND a.isDeleted = false
        AND a.startDate <= :endDate
        AND a.endDate >= :startDate
    """)
    List<Allocation> findOverlappingAllocations(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find overlapping allocations by employee, date and status
    List<Allocation>
    findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusInAndIsDeletedFalse(
            Long employeeId,
            LocalDate endDate,
            LocalDate startDate,
            Collection<AllocationStatus> statuses
    );
}