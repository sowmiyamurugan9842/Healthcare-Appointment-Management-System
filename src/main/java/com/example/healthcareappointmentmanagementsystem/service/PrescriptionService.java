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
     * Creates a new prescription linked to a completed appointment.
     *
     * @param request PrescriptionRequest DTO
     * @return PrescriptionResponse DTO
     */
    PrescriptionResponse createPrescription(PrescriptionRequest request);

    /**
     * Retrieves all prescriptions in the system.
     *
     * @return list of PrescriptionResponse DTOs
     */
    List<PrescriptionResponse> getAllPrescriptions();

    /**
     * Retrieves a prescription by its unique ID.
     *
     * @param id prescription primary key
     * @return PrescriptionResponse DTO
     */
    PrescriptionResponse getPrescriptionById(Long id);

    /**
     * Retrieves the prescription issued for a specific appointment.
     *
     * @param appointmentId appointment primary key
     * @return PrescriptionResponse DTO
     */
    PrescriptionResponse getPrescriptionByAppointment(Long appointmentId);

    /**
     * Retrieves prescriptions scheduled for follow-up on a specific date.
     *
     * @param nextVisitDate target follow-up date
     * @return list of PrescriptionResponse DTOs
     */
    List<PrescriptionResponse> getPrescriptionsByNextVisitDate(LocalDate nextVisitDate);

    /**
     * Retrieves prescriptions with follow-up dates falling within a specific date range.
     *
     * @param startDate range start date
     * @param endDate   range end date
     * @return list of PrescriptionResponse DTOs
     */
    List<PrescriptionResponse> getPrescriptionsBetweenDates(LocalDate startDate, LocalDate endDate);

    /**
     * Updates an existing prescription's details (diagnosis, meds, dosage).
     *
     * @param id      prescription primary key
     * @param request PrescriptionRequest DTO containing updated parameters
     * @return updated PrescriptionResponse DTO
     */
    PrescriptionResponse updatePrescription(Long id, PrescriptionRequest request);

    /**
     * Deletes a prescription record from the database.
     *
     * @param id prescription primary key
     */
    void deletePrescription(Long id);
}
