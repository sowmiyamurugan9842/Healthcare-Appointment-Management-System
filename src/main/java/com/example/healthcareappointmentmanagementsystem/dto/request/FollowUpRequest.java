package com.example.healthcareappointmentmanagementsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for scheduling or updating a patient follow-up consultation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload to schedule or update a patient follow-up consultation")
public class FollowUpRequest {

    @NotNull(message = "Follow-up date is required")
    @FutureOrPresent(message = "Follow-up date must be in the future or present")
    @Schema(description = "Follow-up consultation date", example = "2026-10-10", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate followUpDate;

    @Schema(description = "Optional follow-up consultation time", example = "10:00:00")
    private LocalTime followUpTime;

    @Size(max = 500, message = "Follow-up notes cannot exceed 500 characters")
    @Schema(description = "Doctor's instructions and notes for the follow-up visit", example = "Review blood pressure and continue medication.")
    private String followUpNotes;
}
