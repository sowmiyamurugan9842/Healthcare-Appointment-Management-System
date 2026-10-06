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
     * Retrieves all appointments booked by the currently authenticated patient.
     *
     * @return list of AppointmentResponse DTOs
     */
    List<AppointmentResponse> getMyAppointmentsForPatient();

    /**
     * Retrieves all appointments assigned to a specific doctor.
     *
     * @param doctorId doctor primary key
     * @return list of AppointmentResponse DTOs
     */
    List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId);

    /**
     * Retrieves all appointments assigned to the currently authenticated doctor.
     *
     * @return list of AppointmentResponse DTOs
     */
    List<AppointmentResponse> getMyAppointmentsForDoctor();

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
     * Confirms all eligible PENDING appointments assigned to a specific doctor.
     * Transitions only PENDING appointments to CONFIRMED.
     * Leaves CONFIRMED, COMPLETED, CANCELLED, REJECTED, and EXPIRED appointments unchanged.
     *
     * @param doctorId doctor primary key
     * @return list of newly confirmed AppointmentResponse DTOs
     */
    List<AppointmentResponse> confirmAllAppointmentsByDoctor(Long doctorId);

    /**
     * Confirms all eligible PENDING appointments assigned to the currently authenticated doctor.
     *
     * @return list of newly confirmed AppointmentResponse DTOs
     */
    List<AppointmentResponse> confirmAllMyAppointments();

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

    /**
     * Automatically identifies and expires past uncompleted appointments.
     * Transitions PENDING and CONFIRMED appointments where appointmentDate < today to EXPIRED.
     *
     * @return count of appointments updated to EXPIRED
     */
    int expireOverdueAppointments();

    /**
     * Automatically scans confirmed appointments and sends 1-hour in-app reminders to patients.
     *
     * @return count of reminders successfully sent
     */
    int sendAppointmentReminders();

    /**
     * Marks a confirmed appointment as NO_SHOW.
     * Accessible by the assigned Doctor or an Admin.
     * Automatically dispatches an in-app notification to the patient and triggers waitlist matching for the freed slot.
     *
     * @param appointmentId appointment ID
     * @param reason        optional justification/reason
     * @return updated AppointmentResponse DTO
     */
    AppointmentResponse markAppointmentAsNoShow(Long appointmentId, String reason);

    /**
     * Retrieves the count of NO_SHOW appointments, with optional filtering by doctor, patient, and date range.
     *
     * @param doctorId  optional Doctor ID
     * @param patientId optional Patient ID
     * @param startDate optional start date filter
     * @param endDate   optional end date filter
     * @return count of matching NO_SHOW appointments
     */
    long getNoShowCount(Long doctorId, Long patientId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * Parameterized version for executing reminder checks with custom timestamp.
     *
     * @param currentDateTime current execution timestamp
     * @return count of reminders successfully sent
     */
    int sendAppointmentReminders(java.time.LocalDateTime currentDateTime);

    /**
     * Schedules or updates a follow-up consultation for an appointment.
     *
     * @param appointmentId appointment primary key
     * @param request       FollowUpRequest containing followUpDate, followUpTime, followUpNotes
     * @param userEmail     authenticated user email
     * @return FollowUpResponse DTO
     */
    com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse setAppointmentFollowUp(Long appointmentId, com.example.healthcareappointmentmanagementsystem.dto.request.FollowUpRequest request, String userEmail);

    /**
     * Retrieves the scheduled follow-up details for an appointment.
     *
     * @param appointmentId appointment primary key
     * @param userEmail     authenticated user email
     * @return FollowUpResponse DTO
     */
    com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse getAppointmentFollowUp(Long appointmentId, String userEmail);

    /**
     * Periodically scans for approaching follow-up dates and sends advance reminders to patients.
     *
     * @return count of follow-up reminders dispatched
     */
    int sendFollowUpReminders();

    /**
     * Parameterized follow-up reminder scan for a specific reference date and advance window.
     *
     * @param referenceDate current execution date
     * @param daysBefore    number of days prior to follow-up to send reminder (default 1)
     * @return count of follow-up reminders dispatched
     */
    int sendFollowUpReminders(java.time.LocalDate referenceDate, int daysBefore);
}
