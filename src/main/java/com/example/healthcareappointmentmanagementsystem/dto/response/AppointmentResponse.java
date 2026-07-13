package com.example.healthcareappointmentmanagementsystem.dto.response;

import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO representing Appointment details returned to the client.
 * Combines details from Appointment, Doctor, Patient, and Department into a flat structure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private Long id;
    private String doctorName;
    private String patientName;
    private String departmentName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String reasonForVisit;
    private AppointmentStatus status;
    private Double consultationFee;
}
