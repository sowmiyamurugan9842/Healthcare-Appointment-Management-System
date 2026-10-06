package com.example.healthcareappointmentmanagementsystem.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for carrying data to book an Appointment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    // Patient ID is optional in the incoming request for PATIENT users (resolved automatically from auth context).
    // For ADMIN bookings, presence of patientId is validated in the service layer.
    private Long patientId;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date must be today or in the future")
    private LocalDate appointmentDate;

    @NotNull(message = "Appointment time is required")
    private LocalTime appointmentTime;

    @NotBlank(message = "Reason for visit is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reasonForVisit;
}
