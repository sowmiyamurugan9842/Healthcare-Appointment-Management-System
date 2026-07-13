package com.example.healthcareappointmentmanagementsystem.controller;

import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;
import com.example.healthcareappointmentmanagementsystem.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller exposing REST endpoints for managing Patient Prescriptions.
 */
@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * Constructor injection. Spring Boot injects PrescriptionService.
     */
    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    /**
     * Endpoint to create a new prescription.
     * Maps to POST /api/prescriptions.
     * Access: DOCTOR only.
     */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> createPrescription(@Valid @RequestBody PrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.createPrescription(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Endpoint to retrieve all prescriptions.
     * Maps to GET /api/prescriptions.
     * Access: ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PrescriptionResponse>> getAllPrescriptions() {
        List<PrescriptionResponse> response = prescriptionService.getAllPrescriptions();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve a specific prescription by ID.
     * Maps to GET /api/prescriptions/{id}.
     * Access: ADMIN, DOCTOR, PATIENT.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(@PathVariable Long id) {
        PrescriptionResponse response = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve the prescription associated with a specific appointment.
     * Maps to GET /api/prescriptions/appointment/{appointmentId}.
     * Access: DOCTOR or PATIENT.
     */
    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<PrescriptionResponse> getPrescriptionByAppointment(@PathVariable Long appointmentId) {
        PrescriptionResponse response = prescriptionService.getPrescriptionByAppointment(appointmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve prescriptions by next visit date.
     * Maps to GET /api/prescriptions/next-visit/{nextVisitDate}.
     * Access: DOCTOR or ADMIN.
     */
    @GetMapping("/next-visit/{nextVisitDate}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByNextVisitDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextVisitDate) {
        List<PrescriptionResponse> response = prescriptionService.getPrescriptionsByNextVisitDate(nextVisitDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve prescriptions between next visit dates.
     * Maps to GET /api/prescriptions/between?startDate={startDate}&endDate={endDate}.
     * Access: DOCTOR or ADMIN.
     */
    @GetMapping("/between")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<PrescriptionResponse> response = prescriptionService.getPrescriptionsBetweenDates(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to update an existing prescription.
     * Maps to PUT /api/prescriptions/{id}.
     * Access: DOCTOR only.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> updatePrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.updatePrescription(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to delete a specific prescription by ID.
     * Maps to DELETE /api/prescriptions/{id}.
     * Access: ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

