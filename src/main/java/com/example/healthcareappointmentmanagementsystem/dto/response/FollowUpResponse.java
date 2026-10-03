package com.example.healthcareappointmentmanagementsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO representing confirmed follow-up consultation details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload containing follow-up consultation details")
public class FollowUpResponse {

    @Schema(description = "Appointment ID", example = "101")
    private Long appointmentId;

    @Schema(description = "Prescription ID (if issued)", example = "50")
    private Long prescriptionId;

    @Schema(description = "Patient ID", example = "4")
    private Long patientId;

    @Schema(description = "Patient full name", example = "Jane Doe")
    private String patientName;

    @Schema(description = "Doctor ID", example = "2")
    private Long doctorId;

    @Schema(description = "Doctor full name", example = "Dr. Priya Nair")
    private String doctorName;

    @Schema(description = "Follow-up consultation date", example = "2026-10-10")
    private LocalDate followUpDate;

    @Schema(description = "Follow-up consultation time", example = "10:00:00")
    private LocalTime followUpTime;

    @Schema(description = "Clinical notes/instructions for follow-up", example = "Review blood pressure and continue medication.")
    private String followUpNotes;

    @Schema(description = "Whether the 1-day advance reminder has been dispatched to patient", example = "false")
    private boolean followUpReminderSent;

    @Schema(description = "Informative status message", example = "Follow-up consultation successfully scheduled for 2026-10-10.")
    private String message;
}
