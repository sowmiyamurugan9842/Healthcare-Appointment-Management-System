package com.example.healthcareappointmentmanagementsystem.repository;

import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Appointment entity.
 * Handles database operations for bookings.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Finds all appointments booked by a specific patient.
     * Used for Patient Appointment History.
     *
     * @param patient Patient entity
     * @return list of Appointments
     */
    List<Appointment> findByPatient(Patient patient);

    /**
     * Finds all appointments assigned to a specific doctor.
     * Used for Doctor Dashboard and Schedules.
     *
     * @param doctor Doctor entity
     * @return list of Appointments
     */
    List<Appointment> findByDoctor(Doctor doctor);

    /**
     * Finds all appointments with a specific status.
     *
     * @param status AppointmentStatus enum
     * @return list of Appointments
     */
    List<Appointment> findByStatus(AppointmentStatus status);

    /**
     * Finds all appointments for a specific doctor with a specific status.
     * Used for Doctor filtering (e.g. pending vs completed appointments).
     *
     * @param doctor Doctor entity
     * @param status AppointmentStatus enum
     * @return list of Appointments
     */
    List<Appointment> findByDoctorAndStatus(Doctor doctor, AppointmentStatus status);

    /**
     * Finds all appointments for a specific patient with a specific status.
     * Used for Patient filtering (e.g. active bookings vs cancelled).
     *
     * @param patient Patient entity
     * @param status  AppointmentStatus enum
     * @return list of Appointments
     */
    List<Appointment> findByPatientAndStatus(Patient patient, AppointmentStatus status);

    /**
     * Finds all appointments scheduled on a specific date.
     *
     * @param appointmentDate booking date
     * @return list of Appointments
     */
    List<Appointment> findByAppointmentDate(LocalDate appointmentDate);

    /**
     * Finds all appointments scheduled for a specific doctor on a specific date.
     * Used for checking doctor availability and avoiding double booking.
     *
     * @param doctor          Doctor entity
     * @param appointmentDate booking date
     * @return list of Appointments
     */
    List<Appointment> findByDoctorAndAppointmentDate(Doctor doctor, LocalDate appointmentDate);

    /**
     * Finds all appointments scheduled for a specific patient on a specific date.
     * Prevents a patient from booking multiple appointments on the same day.
     *
     * @param patient         Patient entity
     * @param appointmentDate booking date
     * @return list of Appointments
     */
    List<Appointment> findByPatientAndAppointmentDate(Patient patient, LocalDate appointmentDate);
}
