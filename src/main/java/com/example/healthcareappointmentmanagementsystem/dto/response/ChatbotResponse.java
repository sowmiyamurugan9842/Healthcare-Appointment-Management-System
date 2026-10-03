package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO representing an AI Chatbot reply returned to the client.
 * Contains both natural language response text and structured action payloads (e.g. slots, doctors, appointments).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotResponse {

    private String message;
    private String replyType; // TEXT, DOCTOR_LIST, SLOTS_LIST, CONFIRMATION_PROMPT, APPOINTMENT_LIST, PRESCRIPTION_LIST, BOOKING_SUCCESS, CANCELLATION_SUCCESS, SAFETY_WARNING
    
    // Structured payloads for rich interactive UI rendering
    private List<DoctorResponse> doctors;
    private List<String> availableSlots;
    private Long appointmentId;
    private Long doctorId;
    private String doctorName;
    private String departmentName;
    private String appointmentDate;
    private String appointmentTime;
    private String reason;
    private List<AppointmentResponse> appointments;
    private List<PrescriptionResponse> prescriptions;
    private List<DepartmentResponse> departments;
    private List<WaitlistResponse> waitlistEntries;
    private WaitlistResponse waitlistEntry;
    
    // State management for multi-turn booking / waitlist
    private String nextAction; // NONE, SELECT_DOCTOR, SELECT_DATE, SELECT_SLOT, CONFIRM_BOOKING, CONFIRM_CANCEL, OFFER_WAITLIST
    private String conversationState;
    private Map<String, Object> context;
}
