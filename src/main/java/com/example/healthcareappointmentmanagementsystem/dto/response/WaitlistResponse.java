package com.example.healthcareappointmentmanagementsystem.dto.response;

import com.example.healthcareappointmentmanagementsystem.entity.WaitlistStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response payload representing a waitlist record with queue metrics and offer status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistResponse {

    @Schema(description = "Waitlist entry ID", example = "1")
    private Long id;

    @Schema(description = "Doctor ID", example = "1")
    private Long doctorId;

    @Schema(description = "Doctor full name", example = "Dr. Admin User")
    private String doctorName;

    @Schema(description = "Doctor specialization", example = "Cardiologist")
    private String doctorSpecialization;

    @Schema(description = "Department name", example = "Cardiology")
    private String departmentName;

    @Schema(description = "Patient ID", example = "4")
    private Long patientId;

    @Schema(description = "Patient full name", example = "John Doe")
    private String patientName;

    @Schema(description = "Patient phone number", example = "+1-555-0199")
    private String patientPhone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Appointment date", example = "2026-10-20")
    private LocalDate appointmentDate;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Patient's preferred time", example = "10:30")
    private LocalTime preferredTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Offered slot time if notified", example = "10:30")
    private LocalTime offeredTime;

    @Schema(description = "Reason for visit", example = "Routine cardiac checkup")
    private String reasonForVisit;

    @Schema(description = "Waitlist lifecycle status", example = "WAITING")
    private WaitlistStatus status;

    @Schema(description = "1-based queue position among waiting entries for this doctor and date", example = "1")
    private Integer queuePosition;

    @Schema(description = "Whether the patient currently has an active pending slot offer", example = "false")
    private boolean offerActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Date and time joined the waitlist")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Time when slot offer was sent")
    private LocalDateTime notifiedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Time when slot offer will expire if unaccepted")
    private LocalDateTime expiresAt;
}
