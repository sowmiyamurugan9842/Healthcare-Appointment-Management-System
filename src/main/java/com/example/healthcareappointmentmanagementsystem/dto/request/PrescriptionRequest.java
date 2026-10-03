package com.example.healthcareappointmentmanagementsystem.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

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

    private String doctorAdvice;

    private String medications;

    private String dosageInstructions;

    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    private String additionalNotes;

    @FutureOrPresent(message = "Next visit date must be in the future or present")
    private LocalDate nextVisitDate;

    private LocalDate followUpDate;

    private java.time.LocalTime followUpTime;

    private String followUpNotes;

    private List<PrescriptionMedicineDto> medicines;
}
