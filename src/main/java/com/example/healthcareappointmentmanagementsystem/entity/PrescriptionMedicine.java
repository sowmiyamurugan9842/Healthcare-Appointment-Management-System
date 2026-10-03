package com.example.healthcareappointmentmanagementsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Embeddable entity representing an individual medication item inside a Prescription.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionMedicine {

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    @Column(name = "dosage", nullable = false)
    private String dosage;

    @Column(name = "frequency", nullable = false)
    private String frequency;

    @Column(name = "duration", nullable = false)
    private String duration;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;
}
