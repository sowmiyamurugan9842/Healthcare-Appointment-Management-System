package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionMedicineDto;
import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class to convert between Prescription entity and Prescription DTOs.
 */
@Component
public class PrescriptionMapper {

    /**
     * Converts a Prescription entity to a PrescriptionResponse DTO.
     *
     * @param prescription Prescription entity
     * @return PrescriptionResponse DTO
     */
    public PrescriptionResponse toResponse(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        Appointment appt = prescription.getAppointment();
        Long apptId = null;
        LocalDate apptDate = null;
        LocalTime apptTime = null;
        Long doctorId = null;
        String doctorName = "";
        String doctorSpecialization = "";
        Long patientId = null;
        String patientName = "";

        if (appt != null) {
            apptId = appt.getId();
            apptDate = appt.getAppointmentDate();
            apptTime = appt.getAppointmentTime();

            // Resolve Doctor details
            Doctor doc = appt.getDoctor();
            if (doc != null) {
                doctorId = doc.getId();
                doctorSpecialization = doc.getSpecialization();
                User docUser = doc.getUser();
                if (docUser != null) {
                    doctorName = "Dr. " + docUser.getFirstName() + " " + docUser.getLastName();
                }
            }

            // Resolve Patient details
            Patient pat = appt.getPatient();
            if (pat != null) {
                patientId = pat.getId();
                User patUser = pat.getUser();
                if (patUser != null) {
                    patientName = patUser.getFirstName() + " " + patUser.getLastName();
                }
            }
        }

        // Map medicines
        List<PrescriptionMedicineDto> medicineDtos = new ArrayList<>();
        if (prescription.getMedicines() != null) {
            medicineDtos = prescription.getMedicines().stream()
                    .map(m -> PrescriptionMedicineDto.builder()
                            .medicineName(m.getMedicineName())
                            .dosage(m.getDosage())
                            .frequency(m.getFrequency())
                            .duration(m.getDuration())
                            .instructions(m.getInstructions())
                            .build())
                    .collect(Collectors.toList());
        }

        LocalDate followUp = prescription.getNextVisitDate();

        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .appointmentId(apptId)
                .patientId(patientId)
                .patientName(patientName)
                .doctorId(doctorId)
                .doctorName(doctorName)
                .doctorSpecialization(doctorSpecialization)
                .diagnosis(prescription.getDiagnosis())
                .medications(prescription.getMedications())
                .dosageInstructions(prescription.getDosageInstructions())
                .doctorAdvice(prescription.getDoctorAdvice() != null ? prescription.getDoctorAdvice() : prescription.getDosageInstructions())
                .additionalNotes(prescription.getAdditionalNotes())
                .nextVisitDate(prescription.getNextVisitDate())
                .followUpDate(followUp)
                .followUpTime(prescription.getFollowUpTime())
                .followUpNotes(prescription.getFollowUpNotes())
                .followUpReminderSent(prescription.isFollowUpReminderSent())
                .appointmentDate(apptDate)
                .appointmentTime(apptTime)
                .medicines(medicineDtos)
                .whatsappStatus(prescription.getWhatsappStatus() != null ? prescription.getWhatsappStatus() : "NOT_CONFIGURED")
                .whatsappSentAt(prescription.getWhatsappSentAt())
                .whatsappError(prescription.getWhatsappError())
                .pdfGeneratedAt(prescription.getPdfGeneratedAt())
                .createdAt(prescription.getCreatedAt())
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

        List<PrescriptionMedicine> entityMedicines = new ArrayList<>();
        StringBuilder medsText = new StringBuilder();
        StringBuilder dosageText = new StringBuilder();

        if (request.getMedicines() != null && !request.getMedicines().isEmpty()) {
            for (PrescriptionMedicineDto dto : request.getMedicines()) {
                entityMedicines.add(PrescriptionMedicine.builder()
                        .medicineName(dto.getMedicineName())
                        .dosage(dto.getDosage())
                        .frequency(dto.getFrequency())
                        .duration(dto.getDuration())
                        .instructions(dto.getInstructions())
                        .build());

                if (medsText.length() > 0) {
                    medsText.append(", ");
                }
                medsText.append(dto.getMedicineName()).append(" (").append(dto.getDosage()).append(")");

                if (dosageText.length() > 0) {
                    dosageText.append("; ");
                }
                dosageText.append(dto.getMedicineName()).append(": ").append(dto.getFrequency()).append(" for ").append(dto.getDuration());
                if (dto.getInstructions() != null && !dto.getInstructions().isBlank()) {
                    dosageText.append(" - ").append(dto.getInstructions());
                }
            }
        }

        String finalMeds = request.getMedications() != null && !request.getMedications().isBlank()
                ? request.getMedications()
                : (medsText.length() > 0 ? medsText.toString() : "Prescribed medications");

        String finalDosage = request.getDosageInstructions() != null && !request.getDosageInstructions().isBlank()
                ? request.getDosageInstructions()
                : (dosageText.length() > 0 ? dosageText.toString() : (request.getDoctorAdvice() != null ? request.getDoctorAdvice() : "As directed"));

        LocalDate followUp = request.getFollowUpDate() != null ? request.getFollowUpDate() : request.getNextVisitDate();

        return Prescription.builder()
                .appointment(appointment)
                .diagnosis(request.getDiagnosis())
                .medications(finalMeds)
                .dosageInstructions(finalDosage)
                .doctorAdvice(request.getDoctorAdvice() != null ? request.getDoctorAdvice() : finalDosage)
                .additionalNotes(request.getAdditionalNotes())
                .nextVisitDate(followUp)
                .followUpTime(request.getFollowUpTime())
                .followUpNotes(request.getFollowUpNotes())
                .medicines(entityMedicines)
                .build();
    }
}
