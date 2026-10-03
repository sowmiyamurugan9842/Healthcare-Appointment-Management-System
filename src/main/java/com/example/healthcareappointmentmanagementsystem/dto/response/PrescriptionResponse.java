package com.example.healthcareappointmentmanagementsystem.dto.response;

import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionMedicineDto;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
    private Long appointmentId;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String diagnosis;
    private String medications;
    private String dosageInstructions;
    private String doctorAdvice;
    private String additionalNotes;
    private LocalDate nextVisitDate;
    private LocalDate followUpDate;
    private LocalTime followUpTime;
    private String followUpNotes;
    private boolean followUpReminderSent;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private List<PrescriptionMedicineDto> medicines;
    private String whatsappStatus;
    private LocalDateTime whatsappSentAt;
    private String whatsappError;
    private LocalDateTime pdfGeneratedAt;
    private LocalDateTime createdAt;
}
