package com.example.healthcareappointmentmanagementsystem.repository;

import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Prescription entity.
 * Handles database operations for patient prescriptions.
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    /**
     * Finds a prescription associated with a specific appointment.
     * Used for retrieving the prescription of a completed session.
     *
     * @param appointment Appointment entity
     * @return Optional containing the Prescription if found
     */
    Optional<Prescription> findByAppointment(Appointment appointment);

    /**
     * Finds all prescriptions scheduled for a follow-up on a specific date.
     *
     * @param nextVisitDate target follow-up date
     * @return list of Prescriptions
     */
    List<Prescription> findByNextVisitDate(LocalDate nextVisitDate);

    /**
     * Finds all prescriptions with follow-up dates falling within a specified date range.
     * Useful for weekly/monthly follow-up schedules.
     *
     * @param startDate range start date
     * @param endDate   range end date
     * @return list of Prescriptions
     */
    List<Prescription> findByNextVisitDateBetween(LocalDate startDate, LocalDate endDate);
}
