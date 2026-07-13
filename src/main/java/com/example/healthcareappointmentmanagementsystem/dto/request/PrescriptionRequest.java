package com.example.healthcareappointmentmanagementsystem.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for carrying data to issue a new medical Prescription.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionRequest {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    @NotBlank(message = "Medications details are required")
    private String medications;

    @NotBlank(message = "Dosage instructions are required")
    private String dosageInstructions;

    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    private String additionalNotes;

    @FutureOrPresent(message = "Next visit date must be in the future or present")
    private LocalDate nextVisitDate;
}
