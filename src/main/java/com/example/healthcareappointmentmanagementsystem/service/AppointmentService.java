package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;

import java.util.List;

/**
 * Service interface handling medical Appointment booking and status transitions.
 */
public interface AppointmentService {

    /**
     * Books a new appointment with default status PENDING.
     *
     * @param request AppointmentRequest DTO
     * @return AppointmentResponse DTO
     */
    AppointmentResponse bookAppointment(AppointmentRequest request);

    /**
     * Retrieves all appointments registered in the system.
     *
     * @return list of AppointmentResponse DTOs
     */
    List<AppointmentResponse> getAllAppointments();

    /**
     * Retrieves a specific appointment by its ID.
     *
     * @param id appointment primary key
     * @return AppointmentResponse DTO
     */
    AppointmentResponse getAppointmentById(Long id);

    /**
     * Retrieves all appointments booked by a specific patient.
     *
     * @param patientId patient primary key
     * @return list of AppointmentResponse DTOs
     */
    List<AppointmentResponse> getAppointmentsByPatient(Long patientId);

    /**
     * Retrieves all appointments assigned to a specific doctor.
     *
     * @param doctorId doctor primary key
     * @return list of AppointmentResponse DTOs
     */
    List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId);

    /**
     * Retrieves all appointments filtered by status (PENDING, CONFIRMED, COMPLETED, CANCELLED).
     *
     * @param status AppointmentStatus enum
     * @return list of AppointmentResponse DTOs
     */
    List<AppointmentResponse> getAppointmentsByStatus(AppointmentStatus status);

    /**
     * Confirms an appointment (transitions status from PENDING to CONFIRMED).
     *
     * @param appointmentId appointment primary key
     * @return updated AppointmentResponse DTO
     */
    AppointmentResponse confirmAppointment(Long appointmentId);

    /**
     * Cancels an appointment (transitions status to CANCELLED).
     *
     * @param appointmentId appointment primary key
     * @return updated AppointmentResponse DTO
     */
    AppointmentResponse cancelAppointment(Long appointmentId);

    /**
     * Completes an appointment (transitions status to COMPLETED).
     *
     * @param appointmentId appointment primary key
     * @return updated AppointmentResponse DTO
     */
    AppointmentResponse completeAppointment(Long appointmentId);

    /**
     * Updates the status of an appointment.
     *
     * @param id     appointment ID
     * @param status target status (APPROVED, REJECTED, COMPLETED)
     * @return updated AppointmentResponse DTO
     */
    AppointmentResponse updateAppointmentStatus(Long id, String status);

    /**
     * Deletes an appointment record from the database.
     *
     * @param id appointment primary key
     */
    void deleteAppointment(Long id);
}
