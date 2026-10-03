package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.response.WhatsAppDeliveryResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.Prescription;
import com.example.healthcareappointmentmanagementsystem.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation for WhatsApp Business Cloud API communication.
 * Communicates strictly from the server-side via REST.
 * Gracefully reports delivery failure or unconfigured status without interrupting transactional workflows.
 */
@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppServiceImpl.class);

    private final String apiUrl;
    private final String accessToken;
    private final String phoneNumberId;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public WhatsAppServiceImpl(
            @Value("${whatsapp.api-url:https://graph.facebook.com/v19.0}") String apiUrl,
            @Value("${whatsapp.access-token:}") String accessToken,
            @Value("${whatsapp.phone-number-id:}") String phoneNumberId,
            @org.springframework.beans.factory.annotation.Autowired(required = false) RestTemplateBuilder restTemplateBuilder) {
        this.apiUrl = apiUrl != null ? apiUrl.trim() : "https://graph.facebook.com/v19.0";
        this.accessToken = accessToken != null ? accessToken.trim() : "";
        this.phoneNumberId = phoneNumberId != null ? phoneNumberId.trim() : "";
        if (restTemplateBuilder != null) {
            this.restTemplate = restTemplateBuilder
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .setReadTimeout(Duration.ofSeconds(10))
                    .build();
        } else {
            this.restTemplate = new RestTemplate();
        }
    }

    /**
     * Testing constructor allowing custom RestTemplate injection.
     */
    public WhatsAppServiceImpl(String apiUrl, String accessToken, String phoneNumberId, RestTemplate restTemplate) {
        this.apiUrl = apiUrl != null ? apiUrl.trim() : "https://graph.facebook.com/v19.0";
        this.accessToken = accessToken != null ? accessToken.trim() : "";
        this.phoneNumberId = phoneNumberId != null ? phoneNumberId.trim() : "";
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean isConfigured() {
        return !accessToken.isEmpty() && !phoneNumberId.isEmpty();
    }

    @Override
    public WhatsAppDeliveryResponse sendPrescriptionPdf(Prescription prescription, byte[] pdfBytes) {
        if (prescription == null) {
            return WhatsAppDeliveryResponse.builder()
                    .deliveryStatus("FAILED")
                    .message("Prescription is null")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        Appointment appt = prescription.getAppointment();
        Patient patient = appt != null ? appt.getPatient() : null;
        Doctor doctor = appt != null ? appt.getDoctor() : null;

        String recipientName = patient != null && patient.getUser() != null
                ? patient.getUser().getFirstName() + " " + patient.getUser().getLastName()
                : "Patient";

        String doctorName = doctor != null && doctor.getUser() != null
                ? "Dr. " + doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName()
                : "Attending Doctor";

        String rawPhone = resolvePatientPhoneNumber(patient);
        String formattedPhone = formatE164PhoneNumber(rawPhone);

        // Check if phone number is available
        if (formattedPhone == null || formattedPhone.isEmpty()) {
            log.warn("Cannot dispatch WhatsApp prescription #{}: Patient phone number is missing or invalid.", prescription.getId());
            return WhatsAppDeliveryResponse.builder()
                    .prescriptionId(prescription.getId())
                    .appointmentId(appt != null ? appt.getId() : null)
                    .recipientName(recipientName)
                    .recipientPhone("N/A")
                    .deliveryStatus("FAILED")
                    .message("Patient phone number is missing or invalid.")
                    .errorDetails("Phone number was null, empty, or unparseable.")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Check if WhatsApp credentials are configured
        if (!isConfigured()) {
            log.info("WhatsApp API credentials not configured in environment. Skipping live network call for Prescription #{}.", prescription.getId());
            return WhatsAppDeliveryResponse.builder()
                    .prescriptionId(prescription.getId())
                    .appointmentId(appt != null ? appt.getId() : null)
                    .recipientName(recipientName)
                    .recipientPhone(formattedPhone)
                    .deliveryStatus("NOT_CONFIGURED")
                    .message("WhatsApp API is not configured in this environment. The prescription PDF has been generated and is ready for secure in-app download.")
                    .errorDetails("WHATSAPP_ACCESS_TOKEN and/or WHATSAPP_PHONE_NUMBER_ID are not set.")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Attempt live dispatch to Meta WhatsApp Cloud API
        try {
            String endpoint = String.format("%s/%s/messages", apiUrl, phoneNumberId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            String messageText = String.format(
                    "Hello %s,\n\n"
                            + "Your digital prescription from %s (Appointment #%d) is ready.\n\n"
                            + "📋 Diagnosis: %s\n"
                            + "📅 Follow-up: %s\n\n"
                            + "You can download your official signed PDF prescription from your CarePortal account anytime.\n\n"
                            + "Thank you for choosing CarePortal Healthcare System.",
                    recipientName,
                    doctorName,
                    appt != null ? appt.getId() : 0,
                    prescription.getDiagnosis() != null ? prescription.getDiagnosis() : "Clinical Consultation",
                    prescription.getNextVisitDate() != null ? prescription.getNextVisitDate().toString() : "As needed / SOS"
            );

            Map<String, Object> textPayload = new HashMap<>();
            textPayload.put("preview_url", false);
            textPayload.put("body", messageText);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            requestBody.put("to", formattedPhone);
            requestBody.put("type", "text");
            requestBody.put("text", textPayload);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            log.info("Dispatching WhatsApp prescription notification to {} for Prescription #{}...", formattedPhone, prescription.getId());
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp prescription #{} successfully delivered to {} (Status: {})",
                        prescription.getId(), formattedPhone, response.getStatusCode());
                return WhatsAppDeliveryResponse.builder()
                        .prescriptionId(prescription.getId())
                        .appointmentId(appt != null ? appt.getId() : null)
                        .recipientName(recipientName)
                        .recipientPhone(formattedPhone)
                        .deliveryStatus("SENT")
                        .message("Prescription successfully delivered to patient's WhatsApp (" + formattedPhone + ").")
                        .timestamp(LocalDateTime.now())
                        .build();
            } else {
                log.warn("WhatsApp API returned non-2xx status: {} for Prescription #{}", response.getStatusCode(), prescription.getId());
                return WhatsAppDeliveryResponse.builder()
                        .prescriptionId(prescription.getId())
                        .appointmentId(appt != null ? appt.getId() : null)
                        .recipientName(recipientName)
                        .recipientPhone(formattedPhone)
                        .deliveryStatus("FAILED")
                        .message("WhatsApp API returned status: " + response.getStatusCode())
                        .errorDetails(response.getBody())
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to deliver WhatsApp message for Prescription #{}: {}", prescription.getId(), e.getMessage());
            return WhatsAppDeliveryResponse.builder()
                    .prescriptionId(prescription.getId())
                    .appointmentId(appt != null ? appt.getId() : null)
                    .recipientName(recipientName)
                    .recipientPhone(formattedPhone)
                    .deliveryStatus("FAILED")
                    .message("WhatsApp delivery failed: " + e.getMessage())
                    .errorDetails(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    private String resolvePatientPhoneNumber(Patient patient) {
        if (patient == null) return null;
        if (patient.getUser() != null && patient.getUser().getPhoneNumber() != null && !patient.getUser().getPhoneNumber().trim().isEmpty()) {
            return patient.getUser().getPhoneNumber().trim();
        }
        if (patient.getEmergencyContact() != null && !patient.getEmergencyContact().trim().isEmpty()) {
            return patient.getEmergencyContact().trim();
        }
        return null;
    }

    /**
     * Converts phone numbers to standard E.164 without leading '+' for WhatsApp Cloud API.
     * e.g. "9876543210" -> "919876543210" (assuming default Indian format for 10 digits)
     * e.g. "+919876543210" -> "919876543210"
     */
    public static String formatE164PhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            return null;
        }

        String digitsOnly = rawPhone.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return null;
        }

        // Standard Indian 10-digit mobile number starting with 6,7,8,9
        if (digitsOnly.length() == 10) {
            return "91" + digitsOnly;
        }

        // If 11 digits starting with 0, strip 0 and prepend 91
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("0")) {
            return "91" + digitsOnly.substring(1);
        }

        // Already has country code (e.g. 12 digits starting with 91, or other country codes)
        return digitsOnly;
    }
}
