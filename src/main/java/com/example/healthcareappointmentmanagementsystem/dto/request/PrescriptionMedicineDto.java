package com.example.healthcareappointmentmanagementsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO representing an individual medicine item in a prescription request or response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionMedicineDto {

    @NotBlank(message = "Medicine name is required")
    private String medicineName;

    @NotBlank(message = "Dosage is required")
    private String dosage;

    @NotBlank(message = "Frequency is required")
    private String frequency;

    @NotBlank(message = "Duration is required")
    private String duration;

    private String instructions;
}
