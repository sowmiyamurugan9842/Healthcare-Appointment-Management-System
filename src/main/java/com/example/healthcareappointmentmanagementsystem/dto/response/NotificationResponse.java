package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for in-app patient notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long patientId;
    private Long appointmentId;
    private String title;
    private String message;
    private String notificationType;
    private boolean isRead;
    private LocalDateTime createdAt;
}
