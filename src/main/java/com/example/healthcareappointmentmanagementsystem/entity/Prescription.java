package com.example.healthcareappointmentmanagementsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a medical Prescription in the system.
 * Link directly to a completed Appointment, capturing diagnosis, medications, and instructions.
 */
@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A prescription is issued for exactly one specific appointment.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    @NotNull(message = "Appointment is required")
    private Appointment appointment;

    @NotBlank(message = "Diagnosis is required")
    @Column(name = "diagnosis", nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @NotBlank(message = "Medications details are required")
    @Column(name = "medications", nullable = false, columnDefinition = "TEXT")
    private String medications;

    @NotBlank(message = "Dosage instructions are required")
    @Column(name = "dosage_instructions", nullable = false, columnDefinition = "TEXT")
    private String dosageInstructions;

    // Additional doctor notes can be optional/nullable
    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    // Optional date for follow-up
    @FutureOrPresent(message = "Next visit date must be in the future or present")
    @Column(name = "next_visit_date")
    private LocalDate nextVisitDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
