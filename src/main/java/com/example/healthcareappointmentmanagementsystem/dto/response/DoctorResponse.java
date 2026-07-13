package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.*;

import java.time.LocalTime;

/**
 * DTO representing Doctor details returned to the client.
 * Provides a flattened representation of Doctor, User, and Department.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String departmentName;
    private String qualification;
    private String specialization;
    private Integer experienceYears;
    private Double consultationFee;
    private LocalTime availableFrom;
    private LocalTime availableTo;
}
