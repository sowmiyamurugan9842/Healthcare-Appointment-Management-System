package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.WhatsAppDeliveryResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Prescription;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.DuplicateResourceException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.PrescriptionMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PrescriptionRepository;
import com.example.healthcareappointmentmanagementsystem.service.NotificationService;
import com.example.healthcareappointmentmanagementsystem.service.PrescriptionPdfService;
import com.example.healthcareappointmentmanagementsystem.service.PrescriptionService;
import com.example.healthcareappointmentmanagementsystem.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation handling patient Prescription operations,
 * automated PDF generation, and WhatsApp delivery dispatch.
 */
@Service
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionServiceImpl.class);

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionPdfService prescriptionPdfService;
    private final WhatsAppService whatsAppService;
    private final NotificationService notificationService;

    /**
     * Primary constructor injection for Spring Boot.
     */
    @Autowired
    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   AppointmentRepository appointmentRepository,
                                   PatientRepository patientRepository,
                                   PrescriptionMapper prescriptionMapper,
                                   PrescriptionPdfService prescriptionPdfService,
                                   WhatsAppService whatsAppService,
                                   NotificationService notificationService) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.prescriptionMapper = prescriptionMapper;
        this.prescriptionPdfService = prescriptionPdfService;
        this.whatsAppService = whatsAppService;
        this.notificationService = notificationService;
    }

    /**
     * Backward-compatible constructor for legacy unit tests.
     */
    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   AppointmentRepository appointmentRepository,
                                   PatientRepository patientRepository,
                                   PrescriptionMapper prescriptionMapper) {
        this(prescriptionRepository, appointmentRepository, patientRepository, prescriptionMapper, null, null, null);
    }

    @Override
    public PrescriptionResponse createPrescription(PrescriptionRequest request) {
        // Step 1: Find Appointment
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));

        // Step 2: Verify Appointment status == COMPLETED
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot create a prescription for an uncompleted appointment. Current status: " + appointment.getStatus());
        }

        // Step 3: Authorization check - if caller is Doctor, must be the assigned doctor
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
            boolean isDoctor = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR"));
            String email = authentication.getName();

            if (isDoctor && !isAdmin) {
                if (appointment.getDoctor() == null || appointment.getDoctor().getUser() == null ||
                        !appointment.getDoctor().getUser().getEmail().equalsIgnoreCase(email)) {
                    throw new BadRequestException("You are only authorized to issue prescriptions for your own assigned appointments.");
                }
            }
        }

        // Step 4: Check whether a Prescription already exists for this Appointment
        if (prescriptionRepository.findByAppointment(appointment).isPresent()) {
            throw new DuplicateResourceException("A prescription has already been issued for appointment ID: " + request.getAppointmentId());
        }

        // Step 5: Convert DTO to Entity using Mapper
        Prescription prescription = prescriptionMapper.toEntity(request, appointment);
        prescription.setWhatsappStatus("PENDING");

        // Step 6: Save Prescription initially to ensure persistence
        Prescription savedPrescription = prescriptionRepository.save(prescription);
        log.info("Persisted Prescription #{} for Appointment #{}", savedPrescription.getId(), appointment.getId());

        // Step 7: AUTOMATION - Generate PDF & Attempt WhatsApp delivery (Non-rollback safe)
        byte[] pdfBytes = null;
        if (prescriptionPdfService != null) {
            try {
                pdfBytes = prescriptionPdfService.generatePrescriptionPdf(savedPrescription);
                savedPrescription.setPdfGeneratedAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to generate PDF for Prescription #{}: {}", savedPrescription.getId(), e.getMessage());
            }
        }

        // Step 8: WhatsApp Delivery Dispatch (Non-rollback safe)
        String deliveryStatus = "NOT_CONFIGURED";
        if (whatsAppService != null) {
            try {
                WhatsAppDeliveryResponse deliveryResponse = whatsAppService.sendPrescriptionPdf(savedPrescription, pdfBytes);
                deliveryStatus = deliveryResponse.getDeliveryStatus();
                savedPrescription.setWhatsappStatus(deliveryStatus);
                if ("SENT".equalsIgnoreCase(deliveryStatus)) {
                    savedPrescription.setWhatsappSentAt(LocalDateTime.now());
                    savedPrescription.setWhatsappError(null);
                } else {
                    savedPrescription.setWhatsappError(deliveryResponse.getErrorDetails() != null
                            ? deliveryResponse.getErrorDetails()
                            : deliveryResponse.getMessage());
                }
            } catch (Exception e) {
                log.error("WhatsApp delivery attempt failed for Prescription #{}: {}", savedPrescription.getId(), e.getMessage());
                savedPrescription.setWhatsappStatus("FAILED");
                savedPrescription.setWhatsappError(e.getMessage());
                deliveryStatus = "FAILED";
            }
        } else {
            savedPrescription.setWhatsappStatus("NOT_CONFIGURED");
            savedPrescription.setWhatsappError("WhatsApp service is not available.");
        }

        // Step 9: Save updated delivery & PDF metadata
        savedPrescription = prescriptionRepository.save(savedPrescription);

        // Step 10: Synchronize follow-up fields onto Appointment
        if (savedPrescription.getNextVisitDate() != null) {
            appointment.setFollowUpDate(savedPrescription.getNextVisitDate());
            appointment.setFollowUpTime(savedPrescription.getFollowUpTime());
            appointment.setFollowUpNotes(savedPrescription.getFollowUpNotes());
            appointment.setFollowUpReminderSent(false);
            appointmentRepository.save(appointment);
        }

        // Step 11: In-app Notification for Patient
        if (notificationService != null) {
            try {
                notificationService.createPrescriptionNotification(savedPrescription, deliveryStatus);
            } catch (Exception e) {
                log.warn("Failed to create in-app prescription notification: {}", e.getMessage());
            }
        }

        // Step 12: Return Response DTO
        return prescriptionMapper.toResponse(savedPrescription);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPrescriptionPdf(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        // Security authorization check
        validatePrescriptionAccess(prescription);

        if (prescriptionPdfService == null) {
            throw new RuntimeException("Prescription PDF Service is not configured");
        }

        return prescriptionPdfService.generatePrescriptionPdf(prescription);
    }

    @Override
    public WhatsAppDeliveryResponse resendWhatsApp(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        // Only Doctor (assigned) or Admin can trigger WhatsApp delivery
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isDoctor = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR"));
        String email = authentication.getName();

        if (isDoctor && !isAdmin) {
            Appointment appt = prescription.getAppointment();
            if (appt == null || appt.getDoctor() == null || appt.getDoctor().getUser() == null ||
                    !appt.getDoctor().getUser().getEmail().equalsIgnoreCase(email)) {
                throw new UnauthorizedException("You are only authorized to dispatch WhatsApp prescriptions for your own appointments.");
            }
        } else if (!isAdmin) {
            throw new UnauthorizedException("Patients are not authorized to trigger WhatsApp prescription dispatch.");
        }

        byte[] pdfBytes = null;
        if (prescriptionPdfService != null) {
            try {
                pdfBytes = prescriptionPdfService.generatePrescriptionPdf(prescription);
                prescription.setPdfGeneratedAt(LocalDateTime.now());
            } catch (Exception e) {
                log.warn("PDF generation warning during WhatsApp resend: {}", e.getMessage());
            }
        }

        if (whatsAppService == null) {
            prescription.setWhatsappStatus("NOT_CONFIGURED");
            prescription.setWhatsappError("WhatsApp service is not available.");
            prescriptionRepository.save(prescription);
            return WhatsAppDeliveryResponse.builder()
                    .prescriptionId(id)
                    .deliveryStatus("NOT_CONFIGURED")
                    .message("WhatsApp service is not initialized.")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        WhatsAppDeliveryResponse response = whatsAppService.sendPrescriptionPdf(prescription, pdfBytes);
        prescription.setWhatsappStatus(response.getDeliveryStatus());
        if ("SENT".equalsIgnoreCase(response.getDeliveryStatus())) {
            prescription.setWhatsappSentAt(LocalDateTime.now());
            prescription.setWhatsappError(null);
        } else {
            prescription.setWhatsappError(response.getErrorDetails() != null ? response.getErrorDetails() : response.getMessage());
        }

        prescriptionRepository.save(prescription);
        return response;
    }

    private void validatePrescriptionAccess(Prescription prescription) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (isAdmin) {
            return; // Admins have full access
        }

        boolean isDoctor = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR"));
        boolean isPatient = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PATIENT"));
        String email = authentication.getName();

        Appointment appt = prescription.getAppointment();
        if (appt == null) {
            throw new ResourceNotFoundException("Associated appointment not found for prescription");
        }

        if (isDoctor) {
            if (appt.getDoctor() == null || appt.getDoctor().getUser() == null ||
                    !appt.getDoctor().getUser().getEmail().equalsIgnoreCase(email)) {
                throw new UnauthorizedException("You are not authorized to access this prescription.");
            }
            return;
        }

        if (isPatient) {
            if (appt.getPatient() == null || appt.getPatient().getUser() == null ||
                    !appt.getPatient().getUser().getEmail().equalsIgnoreCase(email)) {
                throw new UnauthorizedException("You are not authorized to access another patient's prescription.");
            }
            return;
        }

        throw new UnauthorizedException("User role is not authorized to access prescriptions.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getAllPrescriptions() {
        List<Prescription> prescriptions = prescriptionRepository.findAll();
        return prescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));
        return prescriptionMapper.toResponse(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionByAppointment(Long appointmentId) {
        // Find Appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Find Prescription by Appointment
        Prescription prescription = prescriptionRepository.findByAppointment(appointment)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found for appointment ID: " + appointmentId));

        return prescriptionMapper.toResponse(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsByPatient(Long patientId) {
        // Verify patient exists
        patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        List<Prescription> prescriptions = prescriptionRepository.findByAppointment_Patient_Id(patientId);
        return prescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsByDoctor(Long doctorId) {
        List<Prescription> prescriptions = prescriptionRepository.findByAppointment_Doctor_Id(doctorId);
        return prescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsByNextVisitDate(LocalDate nextVisitDate) {
        List<Prescription> prescriptions = prescriptionRepository.findByNextVisitDate(nextVisitDate);
        return prescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsBetweenDates(LocalDate startDate, LocalDate endDate) {
        List<Prescription> prescriptions = prescriptionRepository.findByNextVisitDateBetween(startDate, endDate);
        return prescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PrescriptionResponse updatePrescription(Long id, PrescriptionRequest request) {
        // Find existing Prescription
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        // Update editable fields
        prescription.setDiagnosis(request.getDiagnosis());
        if (request.getDoctorAdvice() != null) {
            prescription.setDoctorAdvice(request.getDoctorAdvice());
            prescription.setDosageInstructions(request.getDoctorAdvice());
        }
        if (request.getAdditionalNotes() != null) {
            prescription.setAdditionalNotes(request.getAdditionalNotes());
        }
        if (request.getFollowUpDate() != null) {
            prescription.setNextVisitDate(request.getFollowUpDate());
        } else if (request.getNextVisitDate() != null) {
            prescription.setNextVisitDate(request.getNextVisitDate());
        }
        if (request.getFollowUpTime() != null) {
            prescription.setFollowUpTime(request.getFollowUpTime());
        }
        if (request.getFollowUpNotes() != null) {
            prescription.setFollowUpNotes(request.getFollowUpNotes());
        }

        // Save updates
        Prescription updatedPrescription = prescriptionRepository.save(prescription);

        // Synchronize follow-up onto Appointment
        Appointment appt = updatedPrescription.getAppointment();
        if (appt != null && updatedPrescription.getNextVisitDate() != null) {
            appt.setFollowUpDate(updatedPrescription.getNextVisitDate());
            appt.setFollowUpTime(updatedPrescription.getFollowUpTime());
            appt.setFollowUpNotes(updatedPrescription.getFollowUpNotes());
            appt.setFollowUpReminderSent(false);
            appointmentRepository.save(appt);
        }

        return prescriptionMapper.toResponse(updatedPrescription);
    }

    @Override
    @Transactional
    public com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse setPrescriptionFollowUp(
            Long prescriptionId,
            com.example.healthcareappointmentmanagementsystem.dto.request.FollowUpRequest request,
            String userEmail) {
        if (request == null || request.getFollowUpDate() == null) {
            throw new BadRequestException("Follow-up date is required");
        }

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + prescriptionId));

        Appointment appt = prescription.getAppointment();
        if (appt != null && appt.getAppointmentDate() != null && request.getFollowUpDate().isBefore(appt.getAppointmentDate())) {
            throw new BadRequestException("Follow-up date cannot be before original consultation date (" + appt.getAppointmentDate() + ").");
        }

        if (request.getFollowUpDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Follow-up date cannot be in the past.");
        }

        // Authorization check
        if (userEmail != null && !userEmail.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin && appt != null && appt.getDoctor() != null && appt.getDoctor().getUser() != null) {
                if (!appt.getDoctor().getUser().getEmail().equalsIgnoreCase(userEmail)) {
                    throw new UnauthorizedException("You are not authorized to modify follow-up for this prescription.");
                }
            }
        }

        prescription.setNextVisitDate(request.getFollowUpDate());
        prescription.setFollowUpTime(request.getFollowUpTime());
        prescription.setFollowUpNotes(request.getFollowUpNotes());
        prescription.setFollowUpReminderSent(false);
        Prescription savedPx = prescriptionRepository.save(prescription);

        if (appt != null) {
            appt.setFollowUpDate(request.getFollowUpDate());
            appt.setFollowUpTime(request.getFollowUpTime());
            appt.setFollowUpNotes(request.getFollowUpNotes());
            appt.setFollowUpReminderSent(false);
            appointmentRepository.save(appt);
        }

        String doctorName = appt != null && appt.getDoctor() != null && appt.getDoctor().getUser() != null
                ? "Dr. " + appt.getDoctor().getUser().getFirstName() + " " + appt.getDoctor().getUser().getLastName()
                : "Doctor";

        String patientName = appt != null && appt.getPatient() != null && appt.getPatient().getUser() != null
                ? appt.getPatient().getUser().getFirstName() + " " + appt.getPatient().getUser().getLastName()
                : "Patient";

        log.info("Follow-up updated on Prescription #{} for Appointment #{} (FollowUpDate: {})",
                savedPx.getId(), appt != null ? appt.getId() : null, savedPx.getNextVisitDate());

        return com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse.builder()
                .prescriptionId(savedPx.getId())
                .appointmentId(appt != null ? appt.getId() : null)
                .patientId(appt != null && appt.getPatient() != null ? appt.getPatient().getId() : null)
                .patientName(patientName)
                .doctorId(appt != null && appt.getDoctor() != null ? appt.getDoctor().getId() : null)
                .doctorName(doctorName)
                .followUpDate(savedPx.getNextVisitDate())
                .followUpTime(savedPx.getFollowUpTime())
                .followUpNotes(savedPx.getFollowUpNotes())
                .followUpReminderSent(savedPx.isFollowUpReminderSent())
                .message("Follow-up consultation successfully updated for " + savedPx.getNextVisitDate() + ".")
                .build();
    }

    @Override
    public void deletePrescription(Long id) {
        // Find Prescription
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        // Delete Prescription
        prescriptionRepository.delete(prescription);
    }
}
