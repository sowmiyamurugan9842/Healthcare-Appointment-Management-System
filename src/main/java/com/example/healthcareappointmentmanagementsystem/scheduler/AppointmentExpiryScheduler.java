package com.example.healthcareappointmentmanagementsystem.scheduler;

import com.example.healthcareappointmentmanagementsystem.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for automated appointment lifecycle management.
 * Periodically scans the database to transition past-due, uncompleted appointments to EXPIRED.
 */
@Component
public class AppointmentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentExpiryScheduler.class);

    private final AppointmentService appointmentService;

    public AppointmentExpiryScheduler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Periodically runs (every hour, with an initial startup run after 5 seconds)
     * to automatically expire appointments whose scheduled date is prior to today.
     */
    @Scheduled(fixedRateString = "${app.scheduler.appointment-expiry.rate:3600000}",
               initialDelayString = "${app.scheduler.appointment-expiry.initial-delay:5000}")
    public void scheduleAppointmentExpiry() {
        log.info("Running automatic appointment expiry scheduled task...");
        try {
            int expiredCount = appointmentService.expireOverdueAppointments();
            log.info("Automatic appointment expiry completed. {} appointment(s) updated to EXPIRED.", expiredCount);
        } catch (Exception e) {
            log.error("Error occurred while executing automatic appointment expiry job", e);
        }
    }
}
