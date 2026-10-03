package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.*;

import java.time.LocalDate;

/**
 * Response DTO returning aggregate no-show statistics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoShowCountResponse {

    private long totalNoShows;
    private Long doctorId;
    private Long patientId;
    private LocalDate startDate;
    private LocalDate endDate;
}
