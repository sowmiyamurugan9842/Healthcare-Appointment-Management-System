package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.response.WhatsAppDeliveryResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Prescription;

/**
 * Service interface for dispatching medical prescriptions via WhatsApp Business Cloud API.
 */
public interface WhatsAppService {

    /**
     * Attempts to send a medical prescription notification/document to the patient's WhatsApp phone number.
     *
     * @param prescription the persisted clinical Prescription entity
     * @param pdfBytes     the rendered PDF byte array
     * @return WhatsAppDeliveryResponse containing status (SENT, FAILED, NOT_CONFIGURED)
     */
    WhatsAppDeliveryResponse sendPrescriptionPdf(Prescription prescription, byte[] pdfBytes);

    /**
     * Checks if WhatsApp Business Cloud API is actively configured with valid credentials.
     *
     * @return true if access token and phone number ID are present
     */
    boolean isConfigured();
}
