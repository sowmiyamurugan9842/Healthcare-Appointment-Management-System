package com.example.healthcareappointmentmanagementsystem.mapper;

import com.example.healthcareappointmentmanagementsystem.dto.request.PatientRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PatientResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class to convert between Patient entity and Patient DTOs.
 */
@Component
public class PatientMapper {

    /**
     * Converts a Patient entity to a PatientResponse DTO.
     * Maps user credentials into a flat structure.
     *
     * @param patient Patient entity
     * @return PatientResponse DTO
     */
    public PatientResponse toResponse(Patient patient) {
        if (patient == null) {
            return null;
        }

        User user = patient.getUser();
        String fullName = "";
        String email = "";
        String phone = "";
        if (user != null) {
            fullName = user.getFirstName() + " " + user.getLastName();
            email = user.getEmail();
            phone = user.getPhoneNumber();
        }

        return PatientResponse.builder()
                .id(patient.getId())
                .fullName(fullName)
                .email(email)
                .phoneNumber(phone)
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .bloodGroup(patient.getBloodGroup())
                .address(patient.getAddress())
                .emergencyContact(patient.getEmergencyContact())
                .allergies(patient.getAllergies())
                .medicalHistory(patient.getMedicalHistory())
                .build();
    }

    /**
     * Converts a PatientRequest DTO to a Patient entity using provided User.
     *
     * @param request PatientRequest DTO
     * @param user    associated User entity
     * @return Patient entity
     */
    public Patient toEntity(PatientRequest request, User user) {
        if (request == null) {
            return null;
        }

        return Patient.builder()
                .user(user)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .address(request.getAddress())
                .emergencyContact(request.getEmergencyContact())
                .allergies(request.getAllergies())
                .medicalHistory(request.getMedicalHistory())
                .build();
    }
}
