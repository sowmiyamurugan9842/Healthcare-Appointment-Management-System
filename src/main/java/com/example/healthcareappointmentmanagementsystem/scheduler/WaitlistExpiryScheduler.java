package com.example.healthcareappointmentmanagementsystem.scheduler;

import com.example.healthcareappointmentmanagementsystem.service.WaitlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to manage waitlist slot offer expirations and automatic queue progression.
 */
@Component
public class WaitlistExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WaitlistExpiryScheduler.class);

    private final WaitlistService waitlistService;

    public WaitlistExpiryScheduler(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    /**
     * Scans for expired waitlist offers every 60 seconds and promotes the next waiting patient.
     */
    @Scheduled(fixedRateString = "${app.scheduler.waitlist-expiry.rate:60000}",
               initialDelayString = "${app.scheduler.waitlist-expiry.initial-delay:10000}")
    public void scheduleWaitlistOfferExpiry() {
        try {
            int reallocated = waitlistService.checkAndExpirePendingOffers();
            if (reallocated > 0) {
                log.info("Waitlist offer expiry scan: {} unaccepted offer(s) expired and reallocated to next patient(s).", reallocated);
            }
        } catch (Exception e) {
            log.error("Error occurred while processing waitlist offer expirations", e);
        }
    }
}
