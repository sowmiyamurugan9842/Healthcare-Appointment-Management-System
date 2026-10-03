package com.example.healthcareappointmentmanagementsystem.scheduler;

import com.example.healthcareappointmentmanagementsystem.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled automation for advance follow-up consultation reminders.
 * Automatically scans the database for scheduled follow-ups and dispatches
 * exactly one advance notification to the patient before their follow-up date.
 */
@Component
public class FollowUpReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(FollowUpReminderScheduler.class);

    private final AppointmentService appointmentService;

    public FollowUpReminderScheduler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Periodically runs (daily, with an initial startup run after 10 seconds)
     * to dispatch 1-day advance follow-up reminders to patients.
     */
    @Scheduled(fixedRateString = "${app.scheduler.follow-up-reminder.rate:86400000}",
               initialDelayString = "${app.scheduler.follow-up-reminder.initial-delay:10000}")
    public void scheduleFollowUpReminders() {
        log.info("Running automatic follow-up reminder scheduled task...");
        try {
            int reminderCount = appointmentService.sendFollowUpReminders();
            if (reminderCount > 0) {
                log.info("Follow-up reminder job completed. {} advance reminder(s) successfully created.", reminderCount);
            } else {
                log.debug("No eligible upcoming follow-up reminders to send at this time.");
            }
        } catch (Exception e) {
            log.error("Error occurred while executing automatic follow-up reminder job", e);
        }
    }
}
