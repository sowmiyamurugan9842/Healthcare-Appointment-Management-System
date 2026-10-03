package com.example.healthcareappointmentmanagementsystem.entity;

/**
 * Defines the lifecycle states of a patient waitlist entry.
 */
public enum WaitlistStatus {
    /** Patient is waiting in queue for a matching slot to become available */
    WAITING,
    /** A slot opened up and the patient has been offered the slot (awaiting patient confirmation) */
    NOTIFIED,
    /** Patient accepted the offered slot and an appointment was scheduled */
    BOOKED,
    /** Patient cancelled or removed their waitlist entry */
    CANCELLED,
    /** The offered slot expired without patient confirmation, or date passed */
    EXPIRED
}
