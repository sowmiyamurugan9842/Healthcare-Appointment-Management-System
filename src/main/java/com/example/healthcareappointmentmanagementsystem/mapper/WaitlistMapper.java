package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper class for WaitlistEntry entity and DTO conversions.
 */
@Component
public class WaitlistMapper {

    /**
     * Converts a WaitlistRequest to a WaitlistEntry entity.
     */
    public WaitlistEntry toEntity(WaitlistRequest request, Doctor doctor, Patient patient) {
        if (request == null) {
            return null;
        }

        return WaitlistEntry.builder()
                .doctor(doctor)
                .patient(patient)
                .appointmentDate(request.getAppointmentDate())
                .preferredTime(request.getPreferredTime())
                .reasonForVisit(request.getReasonForVisit())
                .status(WaitlistStatus.WAITING)
                .build();
    }

    /**
     * Converts a WaitlistEntry entity to a WaitlistResponse DTO.
     */
    public WaitlistResponse toResponse(WaitlistEntry entry, Integer queuePosition) {
        if (entry == null) {
            return null;
        }

        Doctor doctor = entry.getDoctor();
        String doctorName = "";
        String specialization = "";
        String departmentName = "";
        Long doctorId = null;

        if (doctor != null) {
            doctorId = doctor.getId();
            specialization = doctor.getSpecialization();
            if (doctor.getUser() != null) {
                doctorName = "Dr. " + doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName();
            }
            if (doctor.getDepartment() != null) {
                departmentName = doctor.getDepartment().getDepartmentName();
            }
        }

        Patient patient = entry.getPatient();
        String patientName = "";
        String patientPhone = "";
        Long patientId = null;

        if (patient != null) {
            patientId = patient.getId();
            if (patient.getUser() != null) {
                patientName = patient.getUser().getFirstName() + " " + patient.getUser().getLastName();
                patientPhone = patient.getUser().getPhoneNumber();
            }
        }

        boolean offerActive = entry.getStatus() == WaitlistStatus.NOTIFIED
                && entry.getExpiresAt() != null
                && entry.getExpiresAt().isAfter(LocalDateTime.now());

        return WaitlistResponse.builder()
                .id(entry.getId())
                .doctorId(doctorId)
                .doctorName(doctorName)
                .doctorSpecialization(specialization)
                .departmentName(departmentName)
                .patientId(patientId)
                .patientName(patientName)
                .patientPhone(patientPhone)
                .appointmentDate(entry.getAppointmentDate())
                .preferredTime(entry.getPreferredTime())
                .offeredTime(entry.getOfferedTime())
                .reasonForVisit(entry.getReasonForVisit())
                .status(entry.getStatus())
                .queuePosition(queuePosition)
                .offerActive(offerActive)
                .createdAt(entry.getCreatedAt())
                .notifiedAt(entry.getNotifiedAt())
                .expiresAt(entry.getExpiresAt())
                .build();
    }
}
