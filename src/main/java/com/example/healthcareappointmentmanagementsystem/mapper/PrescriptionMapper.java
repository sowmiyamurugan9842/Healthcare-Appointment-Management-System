package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Mapper class to convert between Prescription entity and Prescription DTOs.
 */
@Component
public class PrescriptionMapper {

    /**
     * Converts a Prescription entity to a PrescriptionResponse DTO.
     * Navigates nested objects to create a flat DTO structure.
     *
     * @param prescription Prescription entity
     * @return PrescriptionResponse DTO
     */
    public PrescriptionResponse toResponse(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        // 1. Resolve Appointment, Doctor, and Patient context details
        Appointment appt = prescription.getAppointment();
        LocalDate apptDate = null;
        LocalTime apptTime = null;
        String doctorName = "";
        String patientName = "";

        if (appt != null) {
            apptDate = appt.getAppointmentDate();
            apptTime = appt.getAppointmentTime();

            // Resolve Doctor Name
            Doctor doc = appt.getDoctor();
            if (doc != null) {
                User docUser = doc.getUser();
                if (docUser != null) {
                    doctorName = "Dr. " + docUser.getFirstName() + " " + docUser.getLastName();
                }
            }

            // Resolve Patient Name
            Patient pat = appt.getPatient();
            if (pat != null) {
                User patUser = pat.getUser();
                if (patUser != null) {
                    patientName = patUser.getFirstName() + " " + patUser.getLastName();
                }
            }
        }

        // 2. Assemble and return the flat response DTO
        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .doctorName(doctorName)
                .patientName(patientName)
                .diagnosis(prescription.getDiagnosis())
                .medications(prescription.getMedications())
                .dosageInstructions(prescription.getDosageInstructions())
                .additionalNotes(prescription.getAdditionalNotes())
                .nextVisitDate(prescription.getNextVisitDate())
                .appointmentDate(apptDate)
                .appointmentTime(apptTime)
                .build();
    }

    /**
     * Converts a PrescriptionRequest DTO to a Prescription entity.
     *
     * @param request     PrescriptionRequest DTO
     * @param appointment associated Appointment entity
     * @return Prescription entity
     */
    public Prescription toEntity(PrescriptionRequest request, Appointment appointment) {
        if (request == null) {
            return null;
        }

        return Prescription.builder()
                .appointment(appointment)
                .diagnosis(request.getDiagnosis())
                .medications(request.getMedications())
                .dosageInstructions(request.getDosageInstructions())
                .additionalNotes(request.getAdditionalNotes())
                .nextVisitDate(request.getNextVisitDate())
                .build();
    }
}
