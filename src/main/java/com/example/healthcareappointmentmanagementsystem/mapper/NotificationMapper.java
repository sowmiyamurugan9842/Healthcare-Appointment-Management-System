package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.response.NotificationResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Notification;
import org.springframework.stereotype.Component;

/**
 * Mapper component to transform Notification entities into response DTOs.
 */
@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .patientId(notification.getPatient() != null ? notification.getPatient().getId() : null)
                .appointmentId(notification.getAppointment() != null ? notification.getAppointment().getId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
