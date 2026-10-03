package com.example.healthcareappointmentmanagementsystem.repository;

import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.WaitlistEntry;
import com.example.healthcareappointmentmanagementsystem.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA Repository for WaitlistEntry persistence.
 */
@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByPatientOrderByCreatedAtDesc(Patient patient);

    List<WaitlistEntry> findByPatientAndStatusInOrderByCreatedAtDesc(Patient patient, Collection<WaitlistStatus> statuses);

    List<WaitlistEntry> findByDoctorOrderByCreatedAtDesc(Doctor doctor);

    List<WaitlistEntry> findByDoctorAndAppointmentDateOrderByCreatedAtAsc(Doctor doctor, LocalDate appointmentDate);

    List<WaitlistEntry> findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(Doctor doctor, LocalDate appointmentDate, WaitlistStatus status);

    List<WaitlistEntry> findByDoctorAndAppointmentDateAndStatusInOrderByCreatedAtAsc(Doctor doctor, LocalDate appointmentDate, Collection<WaitlistStatus> statuses);

    boolean existsByPatientAndDoctorAndAppointmentDateAndStatusIn(Patient patient, Doctor doctor, LocalDate appointmentDate, Collection<WaitlistStatus> statuses);

    long countByDoctorAndAppointmentDateAndStatusAndCreatedAtBefore(Doctor doctor, LocalDate appointmentDate, WaitlistStatus status, LocalDateTime createdAt);

    List<WaitlistEntry> findByStatusAndExpiresAtBefore(WaitlistStatus status, LocalDateTime dateTime);
}
