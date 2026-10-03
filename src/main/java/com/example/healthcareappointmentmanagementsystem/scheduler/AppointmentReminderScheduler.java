package com.example.healthcareappointmentmanagementsystem.scheduler;

import com.example.healthcareappointmentmanagementsystem.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for automated in-app appointment reminder notifications.
 * Periodically scans upcoming confirmed appointments to send exactly one in-app reminder
 * to the patient 1 hour before the scheduled appointment.
 */
@Component
public class AppointmentReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderScheduler.class);

    private final AppointmentService appointmentService;

    public AppointmentReminderScheduler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Periodically runs (every minute, with an initial startup run after 5 seconds)
     * to dispatch 1-hour appointment reminders to patients.
     */
    @Scheduled(fixedRateString = "${app.scheduler.appointment-reminder.rate:60000}",
               initialDelayString = "${app.scheduler.appointment-reminder.initial-delay:5000}")
    public void scheduleAppointmentReminders() {
        log.info("Running automatic appointment reminder scheduled task...");
        try {
            int reminderCount = appointmentService.sendAppointmentReminders();
            if (reminderCount > 0) {
                log.info("Appointment reminder job completed. {} reminder(s) successfully created.", reminderCount);
            } else {
                log.debug("No eligible appointment reminders to send at this time.");
            }
        } catch (Exception e) {
            log.error("Error occurred while executing automatic appointment reminder job", e);
        }
    }
}
