package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Service interface for Waitlist Automation and slot reallocation.
 */
public interface WaitlistService {

    /**
     * Adds the currently authenticated patient to a doctor's waitlist for a specific date.
     */
    WaitlistResponse joinWaitlist(WaitlistRequest request);

    /**
     * Retrieves all waitlist entries for the currently authenticated patient with queue positions.
     */
    List<WaitlistResponse> getMyWaitlist();

    /**
     * Retrieves all waitlist entries for a specific doctor (Doctor / Admin access).
     */
    List<WaitlistResponse> getWaitlistByDoctor(Long doctorId);

    /**
     * Cancels / leaves an active waitlist entry.
     */
    WaitlistResponse cancelWaitlistEntry(Long id);

    /**
     * Confirms an offered slot and creates the official appointment booking.
     */
    AppointmentResponse confirmOfferedSlot(Long waitlistId);

    /**
     * Automatically matches an opened slot against the waiting queue (FIFO with preferred-time matching).
     *
     * @return 1 if an eligible patient was notified, 0 otherwise.
     */
    int processWaitlistForSlot(Doctor doctor, LocalDate appointmentDate, LocalTime availableSlot);

    /**
     * Scans for unaccepted slot offers that exceeded their expiry window and triggers next eligible patients.
     *
     * @return count of expired offers reallocated.
     */
    int checkAndExpirePendingOffers();
}
