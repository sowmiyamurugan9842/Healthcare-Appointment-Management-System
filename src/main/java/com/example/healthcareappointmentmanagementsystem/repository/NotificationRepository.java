package com.example.healthcareappointmentmanagementsystem.repository;

import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.Notification;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Notification entity.
 * Handles patient notification records and duplicate checking.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByPatientOrderByCreatedAtDesc(Patient patient);

    List<Notification> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    boolean existsByAppointmentId(Long appointmentId);

    boolean existsByAppointmentAndNotificationType(Appointment appointment, String notificationType);

    long countByPatientIdAndIsReadFalse(Long patientId);
}
