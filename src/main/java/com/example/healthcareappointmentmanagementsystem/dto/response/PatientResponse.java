package com.example.healthcareappointmentmanagementsystem.dto.response;

import com.example.healthcareappointmentmanagementsystem.entity.BloodGroup;
import com.example.healthcareappointmentmanagementsystem.entity.Gender;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO representing Patient details returned to the client.
 * Provides a flattened representation of Patient and User.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Gender gender;
    private BloodGroup bloodGroup;
    private String address;
    private String emergencyContact;
    private String allergies;
    private String medicalHistory;
}
