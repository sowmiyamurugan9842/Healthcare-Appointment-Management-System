package com.example.healthcareappointmentmanagementsystem.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request payload for joining a doctor's waitlist.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistRequest {

    @NotNull(message = "Doctor ID is required")
    @Schema(description = "Primary key ID of the doctor", example = "1")
    private Long doctorId;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Target appointment date", example = "2026-10-20")
    private LocalDate appointmentDate;

    @JsonFormat(pattern = "HH:mm[:ss]")
    @Schema(description = "Optional preferred time slot (null for any available time)", example = "10:30:00")
    private LocalTime preferredTime;

    @NotBlank(message = "Reason for visit is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Schema(description = "Clinical reason for consultation", example = "Routine cardiac checkup")
    private String reasonForVisit;
}
