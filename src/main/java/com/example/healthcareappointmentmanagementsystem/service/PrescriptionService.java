package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface handling Prescription creation, retrieval, and updates.
 */
public interface PrescriptionService {

    /**
     * Creates a new prescription for an appointment.
     *
     * @param request PrescriptionRequest DTO
     * @return PrescriptionResponse DTO
     */
    PrescriptionResponse createPrescription(PrescriptionRequest request);

    /**
     * Retrieves all prescriptions in the system (Admin access).
     *
     * @return list of PrescriptionResponse DTOs
     */
    List<PrescriptionResponse> getAllPrescriptions();

    /**
     * Retrieves a specific prescription by ID.
     *
     * @param id Prescription ID
     * @return PrescriptionResponse DTO
     */
    PrescriptionResponse getPrescriptionById(Long id);

    /**
     * Retrieves the prescription for a specific appointment ID.
     *
     * @param appointmentId appointment ID
     * @return PrescriptionResponse DTO
     */
    PrescriptionResponse getPrescriptionByAppointment(Long appointmentId);

    /**
     * Retrieves all prescriptions for a specific patient ID.
     *
     * @param patientId Patient ID
     * @return list of PrescriptionResponse DTOs
     */
    List<PrescriptionResponse> getPrescriptionsByPatient(Long patientId);

    /**
     * Retrieves all prescriptions created by a specific doctor ID.
     *
     * @param doctorId Doctor ID
     * @return list of PrescriptionResponse DTOs
     */
    List<PrescriptionResponse> getPrescriptionsByDoctor(Long doctorId);

    /**
     * Retrieves prescriptions scheduled for a specific follow-up date.
     *
     * @param nextVisitDate target follow-up date
     * @return list of PrescriptionResponse DTOs
     */
    List<PrescriptionResponse> getPrescriptionsByNextVisitDate(LocalDate nextVisitDate);

    /**
     * Retrieves prescriptions with follow-up dates in a specified range.
     *
     * @param startDate range start date
     * @param endDate   range end date
     * @return list of PrescriptionResponse DTOs
     */
    List<PrescriptionResponse> getPrescriptionsBetweenDates(LocalDate startDate, LocalDate endDate);

    /**
     * Updates an existing prescription.
     *
     * @param id      Prescription ID
     * @param request PrescriptionRequest DTO containing updated parameters
     * @return updated PrescriptionResponse DTO
     */
    PrescriptionResponse updatePrescription(Long id, PrescriptionRequest request);

    /**
     * Deletes a prescription by ID.
     *
     * @param id Prescription ID
     */
    void deletePrescription(Long id);

    /**
     * Generates a secure PDF binary document for the requested prescription.
     * Performs strict caller ownership verification for Patients and Doctors.
     *
     * @param id Prescription ID
     * @return PDF byte array
     */
    byte[] getPrescriptionPdf(Long id);

    /**
     * Manually triggers or retries WhatsApp delivery of a prescription PDF.
     * Restricted to authorized Doctors and Admins.
     *
     * @param id Prescription ID
     * @return WhatsAppDeliveryResponse DTO
     */
    com.example.healthcareappointmentmanagementsystem.dto.response.WhatsAppDeliveryResponse resendWhatsApp(Long id);

    /**
     * Sets or updates follow-up consultation details on a prescription.
     * Synchronizes details with the underlying appointment.
     *
     * @param prescriptionId prescription primary key
     * @param request        FollowUpRequest containing followUpDate, followUpTime, followUpNotes
     * @param userEmail      authenticated user email
     * @return FollowUpResponse DTO
     */
    com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse setPrescriptionFollowUp(Long prescriptionId, com.example.healthcareappointmentmanagementsystem.dto.request.FollowUpRequest request, String userEmail);
}
