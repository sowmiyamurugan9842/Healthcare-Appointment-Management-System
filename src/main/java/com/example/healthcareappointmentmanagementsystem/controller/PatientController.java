package com.example.healthcareappointmentmanagementsystem.controller;

import com.example.healthcareappointmentmanagementsystem.dto.request.PatientRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PatientResponse;
import com.example.healthcareappointmentmanagementsystem.entity.BloodGroup;
import com.example.healthcareappointmentmanagementsystem.entity.Gender;
import com.example.healthcareappointmentmanagementsystem.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing REST endpoints for managing Patient profiles.
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    /**
     * Constructor injection. Spring Boot automatically injects PatientService.
     */
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Endpoint to create a new patient profile.
     * Maps to POST /api/patients.
     * Access: ADMIN only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request) {
        PatientResponse response = patientService.createPatient(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Endpoint to retrieve all patient profiles.
     * Maps to GET /api/patients.
     * Access: ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        List<PatientResponse> response = patientService.getAllPatients();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve a specific patient profile by ID.
     * Maps to GET /api/patients/{id}.
     * Access: ADMIN or PATIENT.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable Long id) {
        PatientResponse response = patientService.getPatientById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to filter patients by Blood Group.
     * Maps to GET /api/patients/blood-group/{bloodGroup}.
     * Access: ADMIN only.
     */
    @GetMapping("/blood-group/{bloodGroup}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PatientResponse>> getPatientsByBloodGroup(@PathVariable BloodGroup bloodGroup) {
        List<PatientResponse> response = patientService.getPatientsByBloodGroup(bloodGroup);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to filter patients by Gender.
     * Maps to GET /api/patients/gender/{gender}.
     * Access: ADMIN only.
     */
    @GetMapping("/gender/{gender}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PatientResponse>> getPatientsByGender(@PathVariable Gender gender) {
        List<PatientResponse> response = patientService.getPatientsByGender(gender);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to update an existing patient profile.
     * Maps to PUT /api/patients/{id}.
     * Access: ADMIN or PATIENT.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {
        PatientResponse response = patientService.updatePatient(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to delete a specific patient profile by ID.
     * Maps to DELETE /api/patients/{id}.
     * Access: ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

