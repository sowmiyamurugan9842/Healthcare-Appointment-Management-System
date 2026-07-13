package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO representing Prescription details returned to the client.
 * Combines details from Prescription, Appointment, Doctor, and Patient into a flat structure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {

    private Long id;
    private String doctorName;
    private String patientName;
    private String diagnosis;
    private String medications;
    private String dosageInstructions;
    private String additionalNotes;
    private LocalDate nextVisitDate;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
}
