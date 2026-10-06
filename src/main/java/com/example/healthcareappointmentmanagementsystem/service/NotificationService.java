package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.response.NotificationResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.Notification;

import java.util.List;

/**
 * Service interface for managing patient in-app notifications.
 */
public interface NotificationService {

    List<NotificationResponse> getNotificationsByPatient(Long patientId);

    List<NotificationResponse> getMyNotifications();

    NotificationResponse markAsRead(Long notificationId);

    void markAllAsRead(Long patientId);

    void markAllMyAsRead();

    long getMyUnreadCount();

    Notification createAppointmentReminder(Appointment appointment);

    Notification createWaitlistOfferNotification(com.example.healthcareappointmentmanagementsystem.entity.Patient patient,
                                                 com.example.healthcareappointmentmanagementsystem.entity.Doctor doctor,
                                                 java.time.LocalDate date,
                                                 java.time.LocalTime time,
                                                 int expirationMinutes);

    Notification createNoShowNotification(Appointment appointment);

    Notification createPrescriptionNotification(com.example.healthcareappointmentmanagementsystem.entity.Prescription prescription, String deliveryStatus);

    Notification createFollowUpReminder(Appointment appointment);

    Notification createFollowUpReminder(com.example.healthcareappointmentmanagementsystem.entity.Prescription prescription);

    long getUnreadCount(Long patientId);
}
