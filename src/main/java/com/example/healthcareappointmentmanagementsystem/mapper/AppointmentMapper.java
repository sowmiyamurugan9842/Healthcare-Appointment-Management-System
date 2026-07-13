package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import org.springframework.stereotype.Component;

/**
 * Mapper class to convert between Appointment entity and Appointment DTOs.
 */
@Component
public class AppointmentMapper {

    /**
     * Converts an Appointment entity to an AppointmentResponse DTO.
     * Navigates nested objects to create a flat DTO structure.
     *
     * @param appointment Appointment entity
     * @return AppointmentResponse DTO
     */
    public AppointmentResponse toResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        // 1. Resolve Doctor, Doctor's User name, and consultation fee
        Doctor doctor = appointment.getDoctor();
        String doctorName = "";
        Double fee = 0.0;
        String departmentName = "";
        if (doctor != null) {
            User docUser = doctor.getUser();
            if (docUser != null) {
                doctorName = "Dr. " + docUser.getFirstName() + " " + docUser.getLastName();
            }
            fee = doctor.getConsultationFee();

            // Resolve Department name
            Department dept = doctor.getDepartment();
            if (dept != null) {
                departmentName = dept.getDepartmentName();
            }
        }

        // 2. Resolve Patient name
        Patient patient = appointment.getPatient();
        String patientName = "";
        if (patient != null) {
            User patUser = patient.getUser();
            if (patUser != null) {
                patientName = patUser.getFirstName() + " " + patUser.getLastName();
            }
        }

        // 3. Assemble and return the flat response DTO
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .doctorName(doctorName)
                .patientName(patientName)
                .departmentName(departmentName)
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .reasonForVisit(appointment.getReasonForVisit())
                .status(appointment.getStatus())
                .consultationFee(fee)
                .build();
    }

    /**
     * Converts an AppointmentRequest DTO to an Appointment entity.
     *
     * @param request AppointmentRequest DTO
     * @param doctor  associated Doctor entity
     * @param patient associated Patient entity
     * @return Appointment entity
     */
    public Appointment toEntity(AppointmentRequest request, Doctor doctor, Patient patient) {
        if (request == null) {
            return null;
        }

        return Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .reasonForVisit(request.getReasonForVisit())
                .status(AppointmentStatus.PENDING) // Always PENDING when booked
                .build();
    }
}
