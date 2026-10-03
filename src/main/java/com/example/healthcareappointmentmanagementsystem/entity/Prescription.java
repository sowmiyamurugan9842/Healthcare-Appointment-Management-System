package com.example.healthcareappointmentmanagementsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a medical Prescription in the system.
 * Link directly to a completed Appointment, capturing diagnosis, medications, instructions, and advice.
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

    @Column(name = "medications", columnDefinition = "TEXT")
    private String medications;

    @Column(name = "dosage_instructions", columnDefinition = "TEXT")
    private String dosageInstructions;

    @Column(name = "doctor_advice", columnDefinition = "TEXT")
    private String doctorAdvice;

    // Additional doctor notes can be optional/nullable
    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    // Optional date for follow-up (reusing nextVisitDate column)
    @FutureOrPresent(message = "Next visit date must be in the future or present")
    @Column(name = "next_visit_date")
    private LocalDate nextVisitDate;

    @Column(name = "follow_up_time")
    private java.time.LocalTime followUpTime;

    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    private String followUpNotes;

    @Column(name = "follow_up_reminder_sent", nullable = false)
    @Builder.Default
    private boolean followUpReminderSent = false;

    public LocalDate getFollowUpDate() {
        return nextVisitDate;
    }

    public void setFollowUpDate(LocalDate date) {
        this.nextVisitDate = date;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "prescription_medicines", joinColumns = @JoinColumn(name = "prescription_id"))
    @Builder.Default
    private List<PrescriptionMedicine> medicines = new ArrayList<>();

    // WhatsApp Delivery Tracking
    @Column(name = "whatsapp_status", length = 30)
    @Builder.Default
    private String whatsappStatus = "PENDING"; // PENDING, SENT, FAILED, NOT_CONFIGURED

    @Column(name = "whatsapp_sent_at")
    private LocalDateTime whatsappSentAt;

    @Column(name = "whatsapp_error", columnDefinition = "TEXT")
    private String whatsappError;

    // PDF Generation Timestamp
    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

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
