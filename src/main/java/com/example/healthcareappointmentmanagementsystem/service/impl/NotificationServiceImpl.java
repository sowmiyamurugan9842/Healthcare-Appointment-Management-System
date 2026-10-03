package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.response.NotificationResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.mapper.NotificationMapper;
import com.example.healthcareappointmentmanagementsystem.repository.NotificationRepository;
import com.example.healthcareappointmentmanagementsystem.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing in-app patient notifications and reminders.
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByPatient(Long patientId) {
        return notificationRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));
        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }

    @Override
    public void markAllAsRead(Long patientId) {
        List<Notification> notifications = notificationRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        for (Notification n : notifications) {
            n.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    @Override
    public Notification createAppointmentReminder(Appointment appointment) {
        if (appointment == null || appointment.getPatient() == null) {
            log.warn("Cannot create appointment reminder: Appointment or Patient is null.");
            return null;
        }

        // Format Doctor Name
        String doctorName = "your doctor";
        if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
            String first = appointment.getDoctor().getUser().getFirstName();
            String last = appointment.getDoctor().getUser().getLastName();
            doctorName = (first != null ? first : "") + (last != null ? " " + last : "");
            doctorName = doctorName.trim();
        }
        if (!doctorName.isEmpty() && !doctorName.startsWith("Dr. ") && !doctorName.startsWith("Dr.")) {
            doctorName = "Dr. " + doctorName;
        }

        // Format Time e.g. "10:00 AM"
        String formattedTime = "the scheduled time";
        if (appointment.getAppointmentTime() != null) {
            formattedTime = appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
        }

        String title = "🔔 Appointment Reminder";
        String message = String.format("You have an appointment with %s today at %s. Please be available on time.", doctorName, formattedTime);

        Notification notification = Notification.builder()
                .patient(appointment.getPatient())
                .appointment(appointment)
                .title(title)
                .message(message)
                .notificationType("APPOINTMENT_REMINDER")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created in-app appointment reminder notification #{} for Patient #{} (Appointment #{})",
                saved.getId(), appointment.getPatient().getId(), appointment.getId());
        return saved;
    }

    @Override
    public Notification createWaitlistOfferNotification(com.example.healthcareappointmentmanagementsystem.entity.Patient patient,
                                                        com.example.healthcareappointmentmanagementsystem.entity.Doctor doctor,
                                                        java.time.LocalDate date,
                                                        java.time.LocalTime time,
                                                        int expirationMinutes) {
        if (patient == null || doctor == null) {
            log.warn("Cannot create waitlist offer notification: Patient or Doctor is null.");
            return null;
        }

        String doctorName = "your doctor";
        if (doctor.getUser() != null) {
            String first = doctor.getUser().getFirstName();
            String last = doctor.getUser().getLastName();
            doctorName = (first != null ? first : "") + (last != null ? " " + last : "");
            doctorName = doctorName.trim();
        }
        if (!doctorName.isEmpty() && !doctorName.startsWith("Dr. ") && !doctorName.startsWith("Dr.")) {
            doctorName = "Dr. " + doctorName;
        }

        String formattedTime = time != null ? time.format(DateTimeFormatter.ofPattern("hh:mm a")) : "the specified time";
        String formattedDate = date != null ? date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "the requested date";

        String title = "🎉 Appointment Slot Available!";
        String message = String.format("Good news! A %s appointment slot with %s is now available on %s. You are next on the waitlist. Please confirm within %d minutes.",
                formattedTime, doctorName, formattedDate, expirationMinutes);

        Notification notification = Notification.builder()
                .patient(patient)
                .title(title)
                .message(message)
                .notificationType("WAITLIST_OFFER")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created waitlist slot offer notification #{} for Patient #{} (Doctor #{}, Date {}, Time {})",
                saved.getId(), patient.getId(), doctor.getId(), date, time);
        return saved;
    }

    @Override
    public Notification createNoShowNotification(Appointment appointment) {
        if (appointment == null || appointment.getPatient() == null) {
            log.warn("Cannot create no-show notification: Appointment or Patient is null.");
            return null;
        }

        String doctorName = "your doctor";
        if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
            String first = appointment.getDoctor().getUser().getFirstName();
            String last = appointment.getDoctor().getUser().getLastName();
            doctorName = (first != null ? first : "") + (last != null ? " " + last : "");
            doctorName = doctorName.trim();
        }
        if (!doctorName.isEmpty() && !doctorName.startsWith("Dr. ") && !doctorName.startsWith("Dr.")) {
            doctorName = "Dr. " + doctorName;
        }

        String formattedTime = appointment.getAppointmentTime() != null
                ? appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("hh:mm a"))
                : "the scheduled time";
        String formattedDate = appointment.getAppointmentDate() != null
                ? appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "the scheduled date";

        String title = "⚠️ Appointment Marked as No-Show";
        String message = String.format("Your appointment with %s on %s at %s was marked as a no-show.",
                doctorName, formattedDate, formattedTime);

        Notification notification = Notification.builder()
                .patient(appointment.getPatient())
                .appointment(appointment)
                .title(title)
                .message(message)
                .notificationType("NO_SHOW")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created in-app no-show notification #{} for Patient #{} (Appointment #{})",
                saved.getId(), appointment.getPatient().getId(), appointment.getId());
        return saved;
    }

    @Override
    public Notification createPrescriptionNotification(com.example.healthcareappointmentmanagementsystem.entity.Prescription prescription, String deliveryStatus) {
        if (prescription == null || prescription.getAppointment() == null || prescription.getAppointment().getPatient() == null) {
            log.warn("Cannot create prescription notification: Prescription, Appointment, or Patient is null.");
            return null;
        }

        Appointment appt = prescription.getAppointment();
        Patient patient = appt.getPatient();
        Doctor doctor = appt.getDoctor();

        String doctorName = "your doctor";
        if (doctor != null && doctor.getUser() != null) {
            String first = doctor.getUser().getFirstName();
            String last = doctor.getUser().getLastName();
            doctorName = (first != null ? first : "") + (last != null ? " " + last : "");
            doctorName = doctorName.trim();
        }
        if (!doctorName.isEmpty() && !doctorName.startsWith("Dr. ") && !doctorName.startsWith("Dr.")) {
            doctorName = "Dr. " + doctorName;
        }

        String title = "📋 Digital Prescription Issued";
        String message;
        if ("SENT".equalsIgnoreCase(deliveryStatus)) {
            message = String.format("Your digital prescription from %s (Appointment #%d) is ready and has been sent to your WhatsApp. You can also view and download the PDF in CarePortal.",
                    doctorName, appt.getId());
        } else {
            message = String.format("Your digital prescription from %s (Appointment #%d) is ready. You can download the official signed PDF directly from CarePortal.",
                    doctorName, appt.getId());
        }

        Notification notification = Notification.builder()
                .patient(patient)
                .appointment(appt)
                .title(title)
                .message(message)
                .notificationType("PRESCRIPTION_ISSUED")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created in-app prescription notification #{} for Patient #{} (Prescription #{})",
                saved.getId(), patient.getId(), prescription.getId());
        return saved;
    }

    @Override
    public Notification createFollowUpReminder(Appointment appointment) {
        if (appointment == null || appointment.getPatient() == null) {
            log.warn("Cannot create follow-up reminder: Appointment or Patient is null.");
            return null;
        }

        String doctorName = "your doctor";
        if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
            String first = appointment.getDoctor().getUser().getFirstName();
            String last = appointment.getDoctor().getUser().getLastName();
            doctorName = (first != null ? first : "") + (last != null ? " " + last : "");
            doctorName = doctorName.trim();
        }
        if (!doctorName.isEmpty() && !doctorName.startsWith("Dr. ") && !doctorName.startsWith("Dr.")) {
            doctorName = "Dr. " + doctorName;
        }

        String formattedDate = appointment.getFollowUpDate() != null
                ? appointment.getFollowUpDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "tomorrow";

        String timeStr = appointment.getFollowUpTime() != null
                ? " at " + appointment.getFollowUpTime().format(DateTimeFormatter.ofPattern("hh:mm a"))
                : "";

        String title = "🔔 Follow-up Reminder";
        String message = String.format("Your follow-up consultation with %s is scheduled for tomorrow, %s%s. Please attend your scheduled follow-up.",
                doctorName, formattedDate, timeStr);

        Notification notification = Notification.builder()
                .patient(appointment.getPatient())
                .appointment(appointment)
                .title(title)
                .message(message)
                .notificationType("FOLLOW_UP_REMINDER")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created in-app follow-up reminder notification #{} for Patient #{} (Appointment #{})",
                saved.getId(), appointment.getPatient().getId(), appointment.getId());
        return saved;
    }

    @Override
    public Notification createFollowUpReminder(Prescription prescription) {
        if (prescription == null || prescription.getAppointment() == null) {
            log.warn("Cannot create follow-up reminder: Prescription or Appointment is null.");
            return null;
        }
        return createFollowUpReminder(prescription.getAppointment());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long patientId) {
        return notificationRepository.countByPatientIdAndIsReadFalse(patientId);
    }
}
