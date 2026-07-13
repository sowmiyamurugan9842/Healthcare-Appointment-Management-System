package com.example.healthcareappointmentmanagementsystem.dto.request;

import com.example.healthcareappointmentmanagementsystem.entity.BloodGroup;
import com.example.healthcareappointmentmanagementsystem.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for carrying data to create or update a Patient profile.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Emergency contact is required")
    private String emergencyContact;

    @Size(max = 500, message = "Allergies description cannot exceed 500 characters")
    private String allergies;

    @Size(max = 1000, message = "Medical history description cannot exceed 1000 characters")
    private String medicalHistory;
}
