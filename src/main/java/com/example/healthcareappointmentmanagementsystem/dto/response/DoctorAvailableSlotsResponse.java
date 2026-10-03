package com.example.healthcareappointmentmanagementsystem.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing available appointment slots for a doctor on a specific date.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorAvailableSlotsResponse {

    private Long doctorId;
    private LocalDate date;
    private List<String> availableSlots;
}
