package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO representing the result of a WhatsApp prescription delivery dispatch.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppDeliveryResponse {

    private Long prescriptionId;
    private Long appointmentId;
    private String recipientName;
    private String recipientPhone;
    private String deliveryStatus; // SENT, FAILED, NOT_CONFIGURED, PENDING
    private String message;
    private String errorDetails;
    private LocalDateTime timestamp;
}
