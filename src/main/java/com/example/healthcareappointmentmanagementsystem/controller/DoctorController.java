package com.example.healthcareappointmentmanagementsystem.controller;

import com.example.healthcareappointmentmanagementsystem.dto.request.DoctorRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorResponse;
import com.example.healthcareappointmentmanagementsystem.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing REST endpoints for managing Doctor profiles.
 */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * Constructor injection. Spring Boot automatically injects DoctorService.
     */
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * Endpoint to create a new doctor profile.
     * Maps to POST /api/doctors.
     * Access: ADMIN only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorRequest request) {
        DoctorResponse response = doctorService.createDoctor(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Endpoint to retrieve all doctor profiles.
     * Maps to GET /api/doctors.
     * Access: ADMIN, DOCTOR, PATIENT.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        List<DoctorResponse> response = doctorService.getAllDoctors();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve a specific doctor profile by ID.
     * Maps to GET /api/doctors/{id}.
     * Access: ADMIN, DOCTOR, PATIENT.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long id) {
        DoctorResponse response = doctorService.getDoctorById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve doctors by department.
     * Maps to GET /api/doctors/department/{departmentId}.
     * Access: ADMIN, DOCTOR, PATIENT.
     */
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByDepartment(@PathVariable Long departmentId) {
        List<DoctorResponse> response = doctorService.getDoctorsByDepartment(departmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to search doctors by specialization.
     * Maps to GET /api/doctors/search?specialization={specialization}.
     * Access: ADMIN, DOCTOR, PATIENT.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<List<DoctorResponse>> searchDoctorsBySpecialization(@RequestParam String specialization) {
        List<DoctorResponse> response = doctorService.searchDoctorsBySpecialization(specialization);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to update an existing doctor profile.
     * Maps to PUT /api/doctors/{id}.
     * Access: ADMIN only.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorResponse> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorRequest request) {
        DoctorResponse response = doctorService.updateDoctor(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to delete a specific doctor profile by ID.
     * Maps to DELETE /api/doctors/{id}.
     * Access: ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

