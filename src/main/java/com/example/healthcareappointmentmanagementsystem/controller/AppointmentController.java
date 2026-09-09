package com.example.healthcareappointmentmanagementsystem.controller;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentStatusRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing REST endpoints for scheduling and managing Appointments.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Constructor injection. Spring Boot injects AppointmentService.
     */
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Endpoint to book a new appointment.
     * Maps to POST /api/appointments.
     * Access: PATIENT only.
     */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Endpoint to retrieve all appointments.
     * Maps to GET /api/appointments.
     * Access: ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        List<AppointmentResponse> response = appointmentService.getAllAppointments();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve a specific appointment by ID.
     * Maps to GET /api/appointments/{id}.
     * Access: ADMIN, DOCTOR, PATIENT.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve appointments booked by a specific patient.
     * Maps to GET /api/appointments/patient/{patientId}.
     * Access: PATIENT only.
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByPatient(@PathVariable Long patientId) {
        List<AppointmentResponse> response = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve appointments assigned to a specific doctor.
     * Maps to GET /api/appointments/doctor/{doctorId}.
     * Access: DOCTOR only.
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByDoctor(@PathVariable Long doctorId) {
        List<AppointmentResponse> response = appointmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to filter appointments by status.
     * Maps to GET /api/appointments/status/{status}.
     * Access: ADMIN or DOCTOR.
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByStatus(@PathVariable AppointmentStatus status) {
        List<AppointmentResponse> response = appointmentService.getAppointmentsByStatus(status);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to confirm an appointment.
     * Maps to PUT /api/appointments/{id}/confirm.
     * Access: DOCTOR only.
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<AppointmentResponse> confirmAppointment(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to cancel an appointment.
     * Maps to PUT /api/appointments/{id}/cancel.
     * Access: DOCTOR or PATIENT.
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to complete an appointment.
     * Maps to PUT /api/appointments/{id}/complete.
     * Access: DOCTOR only.
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<AppointmentResponse> completeAppointment(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.completeAppointment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to update the status of an appointment.
     * Maps to PATCH /api/appointments/{id}/status.
     * Access: ADMIN, DOCTOR, PATIENT (with role-based service-level validation).
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<AppointmentResponse> updateAppointmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request) {
        AppointmentResponse response = appointmentService.updateAppointmentStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to delete a specific appointment by ID.
     * Maps to DELETE /api/appointments/{id}.
     * Access: ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

