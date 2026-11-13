
package com.employeeallocation.employee_allocation_backend.controller;

import com.employeeallocation.employee_allocation_backend.dto.availability.AvailabilityRequest;
import com.employeeallocation.employee_allocation_backend.dto.availability.AvailabilityResponse;
import com.employeeallocation.employee_allocation_backend.service.AvailabilityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    // CREATE AVAILABILITY

    @PostMapping("/api/employees/{employeeId}/availability")
    public ResponseEntity<AvailabilityResponse> createAvailability(
            @PathVariable Long employeeId,
            @Valid @RequestBody AvailabilityRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        availabilityService.createAvailability(
                                employeeId,
                                request
                        )
                );
    }

    // GET EMPLOYEE AVAILABILITY

    @GetMapping("/api/employees/{employeeId}/availability")
    public ResponseEntity<List<AvailabilityResponse>> getEmployeeAvailability(
            @PathVariable Long employeeId) {

        return ResponseEntity.ok(
                availabilityService.getEmployeeAvailability(employeeId)
        );
    }

    // GET AVAILABILITY BY ID

    @GetMapping("/api/availability/{id}")
    public ResponseEntity<AvailabilityResponse> getAvailabilityById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                availabilityService.getAvailabilityById(id)
        );
    }

    // UPDATE AVAILABILITY

    @PutMapping("/api/availability/{id}")
    public ResponseEntity<AvailabilityResponse> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest request) {

        return ResponseEntity.ok(
                availabilityService.updateAvailability(id, request)
        );
    }

    // DELETE AVAILABILITY

    @DeleteMapping("/api/availability/{id}")
    public ResponseEntity<Void> deleteAvailability(
            @PathVariable Long id) {

        availabilityService.deleteAvailability(id);

        return ResponseEntity.noContent().build();
    }
}
