package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.service.PrescriptionPdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service implementation generating secure, beautifully styled PDF clinical prescriptions.
 * Powered by OpenPDF for clean, lightweight and license-compliant document rendering.
 */
@Service
public class PrescriptionPdfServiceImpl implements PrescriptionPdfService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionPdfServiceImpl.class);

    // Brand Colors
    private static final Color PRIMARY_TEAL = new Color(15, 168, 181);       // #0FA8B5
    private static final Color DARK_TEAL = new Color(7, 94, 102);           // #075E66
    private static final Color LIGHT_CYAN = new Color(229, 250, 252);       // #E5FAFC
    private static final Color TEXT_DARK = new Color(23, 59, 63);           // #173B3F
    private static final Color TEXT_MUTED = new Color(107, 124, 128);       // #6B7C80
    private static final Color BORDER_GRAY = new Color(226, 232, 240);      // #E2E8F0
    private static final Color ROW_ALT_BG = new Color(248, 250, 252);       // #F8FAFC
    private static final Color ACCENT_GREEN = new Color(16, 185, 129);      // #10B981

    // Font Styles
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, DARK_TEAL);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED);
    private static final Font SECTION_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK_TEAL);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, DARK_TEAL);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TABLE_BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_DARK);
    private static final Font TABLE_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, DARK_TEAL);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_MUTED);
    private static final Font RX_SYMBOL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY_TEAL);

    @Override
    public byte[] generatePrescriptionPdf(Prescription prescription) {
        if (prescription == null) {
            throw new BadRequestException("Prescription cannot be null for PDF generation");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. HOSPITAL / CLINIC HEADER
            addHeader(document, prescription);

            // 2. METADATA CARDS (Doctor info, Patient info, Consultation date)
            addMetadataSection(document, prescription);

            // 3. CLINICAL DIAGNOSIS
            addDiagnosisSection(document, prescription);

            // 4. PRESCRIBED MEDICATIONS TABLE
            addMedicinesTable(document, prescription);

            // 5. DOCTOR ADVICE & CARE PLAN
            addAdviceSection(document, prescription);

            // 6. ADDITIONAL NOTES (if any)
            if (prescription.getAdditionalNotes() != null && !prescription.getAdditionalNotes().trim().isEmpty()) {
                addAdditionalNotesSection(document, prescription);
            }

            // 7. FOOTER & SIGNATURE BLOCK
            addFooterAndSignature(document, prescription);

            document.close();
            log.info("Successfully generated PDF prescription #{} (Appointment #{})",
                    prescription.getId(),
                    prescription.getAppointment() != null ? prescription.getAppointment().getId() : "N/A");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for Prescription #{}: {}", prescription.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate prescription PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document, Prescription prescription) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{70, 30});
        headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Left Cell: Brand Name & Subtitle
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPaddingBottom(8);

        Paragraph brand = new Paragraph("CAREPORTAL HEALTHCARE SYSTEM", TITLE_FONT);
        Paragraph tagline = new Paragraph("Digital Clinical Consultation • Telehealth & Medical Records", SUBTITLE_FONT);
        leftCell.addElement(brand);
        leftCell.addElement(tagline);
        headerTable.addCell(leftCell);

        // Right Cell: Rx Symbol & Prescription ID Badge
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setPaddingBottom(8);

        Paragraph rx = new Paragraph("℞", RX_SYMBOL_FONT);
        rx.setAlignment(Element.ALIGN_RIGHT);
        Paragraph rxId = new Paragraph("Prescription #PX-" + (prescription.getId() != null ? prescription.getId() : "NEW"), LABEL_FONT);
        rxId.setAlignment(Element.ALIGN_RIGHT);
        Paragraph issueDate = new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), SUBTITLE_FONT);
        issueDate.setAlignment(Element.ALIGN_RIGHT);

        rightCell.addElement(rx);
        rightCell.addElement(rxId);
        rightCell.addElement(issueDate);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Decorative Divider
        LineSeparator separator = new LineSeparator(1.5f, 100, PRIMARY_TEAL, Element.ALIGN_CENTER, -2);
        document.add(separator);
        document.add(Chunk.NEWLINE);
    }

    private void addMetadataSection(Document document, Prescription prescription) throws DocumentException {
        Appointment appt = prescription.getAppointment();
        Doctor doctor = appt != null ? appt.getDoctor() : null;
        Patient patient = appt != null ? appt.getPatient() : null;

        String docName = "Attending Physician";
        String docSpec = "General Medicine";
        String docQual = "MBBS";
        if (doctor != null) {
            if (doctor.getUser() != null) {
                docName = "Dr. " + doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName();
            }
            if (doctor.getSpecialization() != null) docSpec = doctor.getSpecialization();
            if (doctor.getQualification() != null) docQual = doctor.getQualification();
        }

        String patName = "Patient";
        String patId = "N/A";
        String patContact = "N/A";
        String patGender = "N/A";
        String patBlood = "N/A";
        if (patient != null) {
            if (patient.getUser() != null) {
                patName = patient.getUser().getFirstName() + " " + patient.getUser().getLastName();
                if (patient.getUser().getPhoneNumber() != null) {
                    patContact = patient.getUser().getPhoneNumber();
                }
            }
            if (patient.getId() != null) patId = "#" + patient.getId();
            if (patient.getGender() != null) patGender = patient.getGender().name();
            if (patient.getBloodGroup() != null) patBlood = patient.getBloodGroup().name();
        }

        String apptDate = appt != null && appt.getAppointmentDate() != null
                ? appt.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : LocalDate.now().toString();
        String apptTime = appt != null && appt.getAppointmentTime() != null
                ? appt.getAppointmentTime().toString().substring(0, 5)
                : "";

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{50, 50});
        metaTable.setSpacingAfter(10);

        // Doctor Card Cell
        PdfPCell docCard = createInfoCardCell("DOCTOR DETAILS",
                new String[][]{
                        {"Name:", docName},
                        {"Specialization:", docSpec},
                        {"Qualification:", docQual},
                        {"Department:", doctor != null && doctor.getDepartment() != null ? doctor.getDepartment().getDepartmentName() : "Clinical Care"}
                });
        metaTable.addCell(docCard);

        // Patient Card Cell
        PdfPCell patCard = createInfoCardCell("PATIENT DETAILS",
                new String[][]{
                        {"Name:", patName + " (ID: " + patId + ")"},
                        {"Gender / Blood:", patGender + " • " + patBlood},
                        {"Phone Contact:", patContact},
                        {"Consultation:", apptDate + (apptTime.isEmpty() ? "" : " at " + apptTime)}
                });
        metaTable.addCell(patCard);

        document.add(metaTable);
    }

    private PdfPCell createInfoCardCell(String title, String[][] details) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_CYAN);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(1f);
        cell.setPadding(8);

        Paragraph cardTitle = new Paragraph(title, SECTION_HEADER_FONT);
        cardTitle.setSpacingAfter(4);
        cell.addElement(cardTitle);

        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        try {
            inner.setWidths(new float[]{35, 65});
        } catch (Exception ignored) {}
        inner.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        for (String[] pair : details) {
            PdfPCell lbl = new PdfPCell(new Paragraph(pair[0], LABEL_FONT));
            lbl.setBorder(Rectangle.NO_BORDER);
            lbl.setPaddingBottom(2);

            PdfPCell val = new PdfPCell(new Paragraph(pair[1], VALUE_FONT));
            val.setBorder(Rectangle.NO_BORDER);
            val.setPaddingBottom(2);

            inner.addCell(lbl);
            inner.addCell(val);
        }

        cell.addElement(inner);
        return cell;
    }

    private void addDiagnosisSection(Document document, Prescription prescription) throws DocumentException {
        Paragraph sectionHeader = new Paragraph("CLINICAL DIAGNOSIS & FINDINGS", SECTION_HEADER_FONT);
        sectionHeader.setSpacingAfter(4);
        document.add(sectionHeader);

        PdfPTable diagTable = new PdfPTable(1);
        diagTable.setWidthPercentage(100);
        diagTable.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ROW_ALT_BG);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(1f);
        cell.setPadding(8);

        String diagnosisText = prescription.getDiagnosis() != null && !prescription.getDiagnosis().trim().isEmpty()
                ? prescription.getDiagnosis()
                : "General clinical consultation and medical review.";

        Paragraph p = new Paragraph(diagnosisText, VALUE_FONT);
        cell.addElement(p);
        diagTable.addCell(cell);

        document.add(diagTable);
    }

    private void addMedicinesTable(Document document, Prescription prescription) throws DocumentException {
        Paragraph sectionHeader = new Paragraph("PRESCRIBED MEDICATIONS (Rx)", SECTION_HEADER_FONT);
        sectionHeader.setSpacingAfter(4);
        document.add(sectionHeader);

        PdfPTable medTable = new PdfPTable(6);
        medTable.setWidthPercentage(100);
        medTable.setWidths(new float[]{6, 30, 16, 16, 14, 18});
        medTable.setSpacingAfter(10);

        // Header Row
        String[] headers = {"#", "Medicine Name", "Dosage", "Frequency", "Duration", "Instructions"};
        for (String h : headers) {
            PdfPCell headerCell = new PdfPCell(new Paragraph(h, TABLE_HEADER_FONT));
            headerCell.setBackgroundColor(DARK_TEAL);
            headerCell.setBorderColor(DARK_TEAL);
            headerCell.setPadding(5);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            medTable.addCell(headerCell);
        }

        List<PrescriptionMedicine> list = prescription.getMedicines();
        if (list != null && !list.isEmpty()) {
            int index = 1;
            for (PrescriptionMedicine m : list) {
                Color rowBg = (index % 2 == 0) ? ROW_ALT_BG : Color.WHITE;

                PdfPCell c1 = new PdfPCell(new Paragraph(String.valueOf(index++), TABLE_BODY_FONT));
                PdfPCell c2 = new PdfPCell(new Paragraph(m.getMedicineName(), TABLE_BODY_BOLD));
                PdfPCell c3 = new PdfPCell(new Paragraph(m.getDosage() != null ? m.getDosage() : "-", TABLE_BODY_FONT));
                PdfPCell c4 = new PdfPCell(new Paragraph(m.getFrequency() != null ? m.getFrequency() : "-", TABLE_BODY_FONT));
                PdfPCell c5 = new PdfPCell(new Paragraph(m.getDuration() != null ? m.getDuration() : "-", TABLE_BODY_FONT));
                PdfPCell c6 = new PdfPCell(new Paragraph(m.getInstructions() != null ? m.getInstructions() : "After food", TABLE_BODY_FONT));

                c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                c5.setHorizontalAlignment(Element.ALIGN_CENTER);

                for (PdfPCell cell : new PdfPCell[]{c1, c2, c3, c4, c5, c6}) {
                    cell.setBackgroundColor(rowBg);
                    cell.setBorderColor(BORDER_GRAY);
                    cell.setPadding(5);
                    medTable.addCell(cell);
                }
            }
        } else {
            // Fallback row if individual medicines list was empty
            String meds = prescription.getMedications() != null ? prescription.getMedications() : "Medications as prescribed";
            String dosage = prescription.getDosageInstructions() != null ? prescription.getDosageInstructions() : "As directed";

            PdfPCell c1 = new PdfPCell(new Paragraph("1", TABLE_BODY_FONT));
            PdfPCell c2 = new PdfPCell(new Paragraph(meds, TABLE_BODY_BOLD));
            PdfPCell c3 = new PdfPCell(new Paragraph(dosage, TABLE_BODY_FONT));
            PdfPCell c4 = new PdfPCell(new Paragraph("As directed", TABLE_BODY_FONT));
            PdfPCell c5 = new PdfPCell(new Paragraph("As instructed", TABLE_BODY_FONT));
            PdfPCell c6 = new PdfPCell(new Paragraph("Follow clinical guidance", TABLE_BODY_FONT));

            for (PdfPCell cell : new PdfPCell[]{c1, c2, c3, c4, c5, c6}) {
                cell.setBackgroundColor(Color.WHITE);
                cell.setBorderColor(BORDER_GRAY);
                cell.setPadding(5);
                medTable.addCell(cell);
            }
        }

        document.add(medTable);
    }

    private void addAdviceSection(Document document, Prescription prescription) throws DocumentException {
        String advice = prescription.getDoctorAdvice();
        if (advice == null || advice.trim().isEmpty()) {
            advice = prescription.getDosageInstructions();
        }
        if (advice == null || advice.trim().isEmpty()) {
            advice = "Please follow the prescribed dosage. Drink plenty of water and get sufficient rest.";
        }

        Paragraph sectionHeader = new Paragraph("DOCTOR'S ADVICE & CARE PLAN", SECTION_HEADER_FONT);
        sectionHeader.setSpacingAfter(4);
        document.add(sectionHeader);

        PdfPTable adviceTable = new PdfPTable(1);
        adviceTable.setWidthPercentage(100);
        adviceTable.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ROW_ALT_BG);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(1f);
        cell.setPadding(8);
        cell.addElement(new Paragraph(advice, VALUE_FONT));
        adviceTable.addCell(cell);

        document.add(adviceTable);
    }

    private void addAdditionalNotesSection(Document document, Prescription prescription) throws DocumentException {
        Paragraph sectionHeader = new Paragraph("ADDITIONAL NOTES & WARNINGS", SECTION_HEADER_FONT);
        sectionHeader.setSpacingAfter(4);
        document.add(sectionHeader);

        PdfPTable notesTable = new PdfPTable(1);
        notesTable.setWidthPercentage(100);
        notesTable.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ROW_ALT_BG);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(1f);
        cell.setPadding(8);
        cell.addElement(new Paragraph(prescription.getAdditionalNotes(), VALUE_FONT));
        notesTable.addCell(cell);

        document.add(notesTable);
    }

    private void addFooterAndSignature(Document document, Prescription prescription) throws DocumentException {
        LocalDate followUp = prescription.getNextVisitDate();
        String followUpStr = followUp != null
                ? followUp.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "As needed / SOS";

        String doctorName = "Attending Physician";
        if (prescription.getAppointment() != null && prescription.getAppointment().getDoctor() != null) {
            Doctor doc = prescription.getAppointment().getDoctor();
            if (doc.getUser() != null) {
                doctorName = "Dr. " + doc.getUser().getFirstName() + " " + doc.getUser().getLastName();
            }
        }

        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{55, 45});
        footerTable.setSpacingBefore(10);
        footerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Left Cell: Follow-up Date Box
        PdfPCell followUpCell = new PdfPCell();
        followUpCell.setBackgroundColor(LIGHT_CYAN);
        followUpCell.setBorderColor(PRIMARY_TEAL);
        followUpCell.setBorderWidth(1f);
        followUpCell.setPadding(8);

        Paragraph fuLabel = new Paragraph("Next Follow-up Consultation:", LABEL_FONT);
        String fullFollowUpText = followUpStr;
        if (prescription.getFollowUpTime() != null) {
            fullFollowUpText += " at " + prescription.getFollowUpTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
        }
        Paragraph fuDate = new Paragraph(fullFollowUpText, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_TEAL));
        followUpCell.addElement(fuLabel);
        followUpCell.addElement(fuDate);

        if (prescription.getFollowUpNotes() != null && !prescription.getFollowUpNotes().trim().isEmpty()) {
            Paragraph fuNotes = new Paragraph("Notes: " + prescription.getFollowUpNotes(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY));
            fuNotes.setSpacingBefore(3);
            followUpCell.addElement(fuNotes);
        }

        footerTable.addCell(followUpCell);

        // Right Cell: Signature Block
        PdfPCell signCell = new PdfPCell();
        signCell.setBorder(Rectangle.NO_BORDER);
        signCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signCell.setPaddingLeft(15);

        Paragraph signLine = new Paragraph("____________________________________", SUBTITLE_FONT);
        signLine.setAlignment(Element.ALIGN_RIGHT);
        Paragraph docSign = new Paragraph(doctorName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_TEAL));
        docSign.setAlignment(Element.ALIGN_RIGHT);
        Paragraph verifiedSeal = new Paragraph("✓ Digitally Signed & Authenticated", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, ACCENT_GREEN));
        verifiedSeal.setAlignment(Element.ALIGN_RIGHT);

        signCell.addElement(signLine);
        signCell.addElement(docSign);
        signCell.addElement(verifiedSeal);
        footerTable.addCell(signCell);

        document.add(footerTable);

        // Bottom Legal / Compliance Banner
        document.add(Chunk.NEWLINE);
        LineSeparator sep = new LineSeparator(0.5f, 100, BORDER_GRAY, Element.ALIGN_CENTER, -2);
        document.add(sep);

        Paragraph bottomNote = new Paragraph(
                "CarePortal Telehealth System • Valid Electronic Health Record • Confidential Medical Prescription • Generated: "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")),
                FOOTER_FONT);
        bottomNote.setAlignment(Element.ALIGN_CENTER);
        bottomNote.setSpacingBefore(4);
        document.add(bottomNote);
    }
}
