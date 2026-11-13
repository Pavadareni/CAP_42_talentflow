
package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository
        extends JpaRepository<Availability, Long> {

    List<Availability> findByEmployeeId(Long employeeId);

    List<Availability>
    findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            LocalDate endDate,
            LocalDate startDate
    );

    List<Availability> findByEmployeeIdOrderByStartDateAsc(
            Long employeeId
    );

    List<Availability>
    findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsDeletedFalse(
            Long employeeId,
            LocalDate endDate,
            LocalDate startDate
    );

    List<Availability>
    findByEmployeeIdAndIsDeletedFalseOrderByStartDateAsc(Long employeeId);
}
