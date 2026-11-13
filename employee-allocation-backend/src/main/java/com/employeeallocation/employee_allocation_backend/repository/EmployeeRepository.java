package com.employeeallocation.employee_allocation_backend.repository;

import com.employeeallocation.employee_allocation_backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByUser_Email(String email);

    boolean existsByEmployeeCode(String employeeCode);

    List<Employee> findByActiveTrue();

    List<Employee> findByDepartmentIgnoreCase(String department);
}