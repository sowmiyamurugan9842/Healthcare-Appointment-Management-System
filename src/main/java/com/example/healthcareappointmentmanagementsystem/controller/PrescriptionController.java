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
     * Access: DOCTOR or ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
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
     * Endpoint to retrieve all prescriptions for the currently authenticated patient.
     * Maps to GET /api/prescriptions/patient/me.
     * Access: PATIENT only.
     */
    @GetMapping("/patient/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<PrescriptionResponse>> getMyPrescriptionsForPatient() {
        List<PrescriptionResponse> response = prescriptionService.getMyPrescriptionsForPatient();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve all prescriptions for a specific patient.
     * Maps to GET /api/prescriptions/patient/{patientId}.
     * Access: ADMIN, DOCTOR, PATIENT.
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByPatient(@PathVariable Long patientId) {
        List<PrescriptionResponse> response = prescriptionService.getPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve the prescription associated with a specific appointment.
     * Maps to GET /api/prescriptions/appointment/{appointmentId}.
     * Access: ADMIN, DOCTOR, PATIENT.
     */
    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<PrescriptionResponse> getPrescriptionByAppointment(@PathVariable Long appointmentId) {
        PrescriptionResponse response = prescriptionService.getPrescriptionByAppointment(appointmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve all prescriptions created by the currently authenticated doctor.
     * Maps to GET /api/prescriptions/doctor/me.
     * Access: DOCTOR only.
     */
    @GetMapping("/doctor/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<PrescriptionResponse>> getMyPrescriptionsForDoctor() {
        List<PrescriptionResponse> response = prescriptionService.getMyPrescriptionsForDoctor();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve all prescriptions created by a specific doctor.
     * Maps to GET /api/prescriptions/doctor/{doctorId}.
     * Access: ADMIN, DOCTOR.
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByDoctor(@PathVariable Long doctorId) {
        List<PrescriptionResponse> response = prescriptionService.getPrescriptionsByDoctor(doctorId);
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
     * Access: DOCTOR or ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
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

    /**
     * Endpoint to download a signed clinical prescription in PDF format.
     * Maps to GET /api/prescriptions/{id}/pdf.
     * Access: PATIENT (own), DOCTOR (own appointment), ADMIN.
     */
    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<byte[]> getPrescriptionPdf(@PathVariable Long id) {
        byte[] pdfBytes = prescriptionService.getPrescriptionPdf(id);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("prescription_" + id + ".pdf")
                .build());
        headers.setCacheControl("no-cache, no-store, must-revalidate");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    /**
     * Endpoint to manually trigger or retry WhatsApp delivery of a prescription PDF.
     * Maps to POST /api/prescriptions/{id}/send-whatsapp.
     * Access: DOCTOR (own appointment) or ADMIN.
     */
    @PostMapping("/{id}/send-whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<com.example.healthcareappointmentmanagementsystem.dto.response.WhatsAppDeliveryResponse> sendWhatsApp(
            @PathVariable Long id) {
        com.example.healthcareappointmentmanagementsystem.dto.response.WhatsAppDeliveryResponse response = prescriptionService.resendWhatsApp(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to schedule or update follow-up consultation details on a prescription.
     * Maps to PUT /api/prescriptions/{id}/follow-up.
     * Access: DOCTOR or ADMIN.
     */
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Schedule or update prescription follow-up",
            description = "Allows an attending doctor or administrator to set follow-up consultation date, time, and instructions for a prescription."
    )
    @PutMapping("/{id}/follow-up")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse> setPrescriptionFollowUp(
            @PathVariable Long id,
            @Valid @RequestBody com.example.healthcareappointmentmanagementsystem.dto.request.FollowUpRequest request,
            java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse response =
                prescriptionService.setPrescriptionFollowUp(id, request, email);
        return ResponseEntity.ok(response);
    }
}
