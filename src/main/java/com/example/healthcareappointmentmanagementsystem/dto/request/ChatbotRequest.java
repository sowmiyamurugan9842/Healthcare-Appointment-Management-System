package com.example.healthcareappointmentmanagementsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

/**
 * DTO representing an incoming patient query or action to the AI Chatbot.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    // Multi-turn conversation context parameters
    private String conversationState;
    private Long selectedDoctorId;
    private String selectedDate;
    private String selectedTime;
    private String reason;
    private Long appointmentIdToCancel;
    private Long waitlistIdToCancel;
    private Long waitlistIdToConfirm;
    private Map<String, Object> context;
}
