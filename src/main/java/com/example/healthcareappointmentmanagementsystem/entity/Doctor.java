package com.example.healthcareappointmentmanagementsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing a Doctor in the system.
 * Holds doctor-specific professional details, availability hours, and mappings to User and Department.
 */
@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A doctor is fundamentally a user of the system.
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @NotNull(message = "User details are required")
    private User user;

    // A doctor belongs to a single medical department.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @NotNull(message = "Department is required")
    private Department department;

    @NotBlank(message = "Qualification is required")
    @Column(name = "qualification", nullable = false)
    private String qualification;

    @NotBlank(message = "Specialization is required")
    @Column(name = "specialization", nullable = false)
    private String specialization;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "0.0", message = "Consultation fee cannot be negative")
    @Column(name = "consultation_fee", nullable = false)
    private Double consultationFee;

    // Available starting hour (e.g., 09:00)
    @NotNull(message = "Availability start time is required")
    @Column(name = "available_from", nullable = false)
    private LocalTime availableFrom;

    // Available ending hour (e.g., 17:00)
    @NotNull(message = "Availability end time is required")
    @Column(name = "available_to", nullable = false)
    private LocalTime availableTo;

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
