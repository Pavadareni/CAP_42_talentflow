package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.allocation.AllocationRequest;
import com.employeeallocation.employee_allocation_backend.dto.allocation.AllocationResponse;
import com.employeeallocation.employee_allocation_backend.enums.AllocationStatus;
import com.employeeallocation.employee_allocation_backend.service.AllocationService;
import com.employeeallocation.employee_allocation_backend.dto.allocation.AllocationRejectionRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping("/allocations")
    @ResponseStatus(HttpStatus.CREATED)
    public AllocationResponse create(
            @Valid @RequestBody AllocationRequest request) {
        return allocationService.createAllocation(request);
    }

    @GetMapping("/allocations")
    public List<AllocationResponse> getAll() {
        return allocationService.getAllAllocations();
    }

    @GetMapping("/allocations/{id}")
    public AllocationResponse getById(@PathVariable Long id) {
        return allocationService.getAllocationById(id);
    }

    @GetMapping("/employees/{employeeId}/allocations")
    public List<AllocationResponse> getByEmployee(
            @PathVariable Long employeeId) {
        return allocationService.getEmployeeAllocations(employeeId);
    }

    @GetMapping("/projects/{projectId}/allocations")
    public List<AllocationResponse> getByProject(
            @PathVariable Long projectId) {
        return allocationService.getProjectAllocations(projectId);
    }

    @PutMapping("/allocations/{id}")
    public AllocationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AllocationRequest request) {
        return allocationService.updateAllocation(id, request);
    }

    @PatchMapping("/allocations/{id}/status")
    public AllocationResponse updateStatus(
            @PathVariable Long id,
            @RequestParam AllocationStatus status) {
        return allocationService.updateStatus(id, status);
    }

    @DeleteMapping("/allocations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        allocationService.softDeleteAllocation(id);
    }

    @PostMapping("/allocations/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AllocationResponse> createBulk(
            @Valid @RequestBody List<@Valid AllocationRequest> requests) {

        return allocationService.createAllocations(requests);
    }

    @GetMapping("/allocations/proposed")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<AllocationResponse> getProposed() {
        return allocationService.getProposedAllocations();
    }

    @PatchMapping("/allocations/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public AllocationResponse approve(@PathVariable Long id) {
        return allocationService.approveAllocation(id);
    }

    @PatchMapping("/allocations/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public AllocationResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody AllocationRejectionRequest request) {

        return allocationService.rejectAllocation(
                id,
                request.reason()
        );
    }
}

