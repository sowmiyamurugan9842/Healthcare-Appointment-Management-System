package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.entity.Prescription;

/**
 * Service interface for generating clinical prescription PDF documents.
 */
public interface PrescriptionPdfService {

    /**
     * Generates a formatted PDF binary representation for a medical prescription.
     *
     * @param prescription the persisted Prescription entity containing clinical data
     * @return byte array containing the generated PDF
     */
    byte[] generatePrescriptionPdf(Prescription prescription);
}
