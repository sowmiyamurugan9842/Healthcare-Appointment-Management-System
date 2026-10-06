package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.NotificationRepository;
import com.example.healthcareappointmentmanagementsystem.service.AppointmentService;
import com.example.healthcareappointmentmanagementsystem.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation handling medical Appointment scheduling and transitions.
 */
@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentMapper appointmentMapper;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final com.example.healthcareappointmentmanagementsystem.service.WaitlistService waitlistService;
    private final com.example.healthcareappointmentmanagementsystem.repository.PrescriptionRepository prescriptionRepository;

    @org.springframework.beans.factory.annotation.Value("${followup.reminder.days-before:1}")
    private int followUpDaysBefore = 1;

    /**
     * Full Constructor injection for Spring Boot.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository,
                                  AppointmentMapper appointmentMapper,
                                  NotificationService notificationService,
                                  NotificationRepository notificationRepository,
                                  @org.springframework.context.annotation.Lazy com.example.healthcareappointmentmanagementsystem.service.WaitlistService waitlistService,
                                  @org.springframework.context.annotation.Lazy com.example.healthcareappointmentmanagementsystem.repository.PrescriptionRepository prescriptionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentMapper = appointmentMapper;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.waitlistService = waitlistService;
        this.prescriptionRepository = prescriptionRepository;
    }

    /**
     * Backward-compatible 7-argument constructor.
     */
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository,
                                  AppointmentMapper appointmentMapper,
                                  NotificationService notificationService,
                                  NotificationRepository notificationRepository,
                                  com.example.healthcareappointmentmanagementsystem.service.WaitlistService waitlistService) {
        this(appointmentRepository, doctorRepository, patientRepository, appointmentMapper, notificationService, notificationRepository, waitlistService, null);
    }

    /**
     * Backward-compatible 6-argument constructor for reminder test harness.
     */
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository,
                                  AppointmentMapper appointmentMapper,
                                  NotificationService notificationService,
                                  NotificationRepository notificationRepository) {
        this(appointmentRepository, doctorRepository, patientRepository, appointmentMapper, notificationService, notificationRepository, null, null);
    }

    /**
     * Backward-compatible 4-argument constructor for legacy test harnesses.
     */
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository,
                                  AppointmentMapper appointmentMapper) {
        this(appointmentRepository, doctorRepository, patientRepository, appointmentMapper, null, null, null);
    }

    @Override
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        if (request == null) {
            throw new BadRequestException("Appointment request cannot be null");
        }

        // Step 1: Find Doctor
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        // Step 2: Resolve Patient securely based on authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
        boolean isPatient = isAuthenticated && authentication.getAuthorities() != null &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        boolean isAdmin = isAuthenticated && authentication.getAuthorities() != null &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Patient patient;
        if (isPatient) {
            // Authenticated PATIENT: Always determine patient from current authenticated user
            String email = authentication.getName();
            patient = patientRepository.findByUser_Email(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + email));

            // Do NOT trust patientId supplied by frontend for PATIENT users
            // Prevent a patient from booking an appointment using another patient's ID
            request.setPatientId(patient.getId());
        } else if (isAdmin) {
            // ADMIN booking on behalf of a selected patient
            if (request.getPatientId() == null) {
                throw new BadRequestException("Patient ID is required");
            }
            patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));
        } else {
            // Non-authenticated harness or test fallback where patientId is explicitly provided in request
            if (request.getPatientId() == null) {
                throw new BadRequestException("Patient ID is required");
            }
            patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));
        }

        // Step 3: Validate appointment date is today or in the future
        if (request.getAppointmentDate() == null || request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Appointment date must be today or in the future");
        }

        // Step 4: Validate appointment time is within doctor's available hours
        LocalTime time = request.getAppointmentTime();
        if (time == null) {
            throw new BadRequestException("Appointment time is required");
        }
        if (time.isBefore(doctor.getAvailableFrom()) || time.isAfter(doctor.getAvailableTo())) {
            throw new BadRequestException("Selected time " + time + " is outside the doctor's available hours: "
                    + doctor.getAvailableFrom() + " to " + doctor.getAvailableTo());
        }

        // Step 5: Check doctor is not already booked for the same date and time
        List<Appointment> doctorAppointments = appointmentRepository.findByDoctorAndAppointmentDate(doctor, request.getAppointmentDate());
        boolean hasDoctorClash = doctorAppointments.stream()
                .anyMatch(a -> a.getAppointmentTime().equals(time)
                        && a.getStatus() != AppointmentStatus.CANCELLED
                        && a.getStatus() != AppointmentStatus.EXPIRED
                        && a.getStatus() != AppointmentStatus.NO_SHOW);
        if (hasDoctorClash) {
            throw new BadRequestException("The doctor is already booked at " + time + " on " + request.getAppointmentDate());
        }

        // Step 6: Check patient does not already have another appointment on the same date
        List<Appointment> patientAppointments = appointmentRepository.findByPatientAndAppointmentDate(patient, request.getAppointmentDate());
        boolean hasPatientClash = patientAppointments.stream()
                .anyMatch(a -> a.getStatus() != AppointmentStatus.CANCELLED
                        && a.getStatus() != AppointmentStatus.EXPIRED
                        && a.getStatus() != AppointmentStatus.NO_SHOW);
        if (hasPatientClash) {
            throw new BadRequestException("The patient already has an appointment booked on " + request.getAppointmentDate());
        }

        // Step 7: Create Appointment entity using Mapper
        Appointment appointment = appointmentMapper.toEntity(request, doctor, patient);

        // Step 8: Set status = PENDING
        appointment.setStatus(AppointmentStatus.PENDING);

        // Step 9: Save Appointment
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Step 10: Return mapped response DTO
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        // Security check: If caller is Patient (and not Admin), they can only view their own appointments
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isPatient = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
            if (isPatient && !isAdmin) {
                String email = auth.getName();
                if (patient.getUser() == null || !patient.getUser().getEmail().equalsIgnoreCase(email)) {
                    throw new UnauthorizedException("You are not authorized to view another patient's appointments.");
                }
            }
        }

        List<Appointment> appointments = appointmentRepository.findByPatient(patient);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointmentsForPatient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UnauthorizedException("User is not authenticated. Please log in.");
        }

        String email = auth.getName();
        Patient patient = patientRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + email));

        List<Appointment> appointments = appointmentRepository.findByPatient(patient);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        // Security check: If caller is a Doctor (and not Admin), they can only view their own appointments
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isDoctor = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
            if (isDoctor && !isAdmin) {
                String email = auth.getName();
                if (doctor.getUser() == null || !doctor.getUser().getEmail().equalsIgnoreCase(email)) {
                    throw new UnauthorizedException("You are not authorized to view another doctor's appointments.");
                }
            }
        }

        List<Appointment> appointments = appointmentRepository.findByDoctor(doctor);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointmentsForDoctor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UnauthorizedException("User is not authenticated. Please log in.");
        }

        String email = auth.getName();
        Doctor doctor = doctorRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user: " + email));

        List<Appointment> appointments = appointmentRepository.findByDoctor(doctor);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByStatus(AppointmentStatus status) {
        List<Appointment> appointments = appointmentRepository.findByStatus(status);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentResponse confirmAppointment(Long appointmentId) {
        // Find appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // State Transition rule: Only PENDING -> CONFIRMED
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING appointments can be confirmed. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    @Transactional
    public List<AppointmentResponse> confirmAllAppointmentsByDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        // Security check: If caller is a Doctor (and not Admin), verify ownership
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isDoctor = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
            if (isDoctor && !isAdmin) {
                String email = auth.getName();
                if (doctor.getUser() == null || !doctor.getUser().getEmail().equalsIgnoreCase(email)) {
                    throw new UnauthorizedException("You are not authorized to confirm appointments for another doctor.");
                }
            }
        }

        List<Appointment> pendingAppointments = appointmentRepository.findByDoctorAndStatus(doctor, AppointmentStatus.PENDING);
        List<AppointmentResponse> confirmedList = new ArrayList<>();

        for (Appointment appointment : pendingAppointments) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            Appointment saved = appointmentRepository.save(appointment);
            confirmedList.add(appointmentMapper.toResponse(saved));
        }

        log.info("Doctor #{} confirmed {} pending appointment(s)", doctorId, confirmedList.size());
        return confirmedList;
    }

    @Override
    @Transactional
    public List<AppointmentResponse> confirmAllMyAppointments() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UnauthorizedException("User is not authenticated. Please log in.");
        }

        String email = auth.getName();
        Doctor doctor = doctorRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user: " + email));

        return confirmAllAppointmentsByDoctor(doctor.getId());
    }

    @Override
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        // Find appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // State Transition rule: Cannot cancel COMPLETED or EXPIRED appointments
        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.EXPIRED) {
            throw new BadRequestException("Cannot cancel an appointment that is already " + appointment.getStatus().name().toLowerCase() + ".");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        triggerWaitlistCheck(savedAppointment);
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    public AppointmentResponse completeAppointment(Long appointmentId) {
        // Find appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // State Transition rule: Only CONFIRMED -> COMPLETED
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED appointments can be completed. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    public AppointmentResponse updateAppointmentStatus(Long id, String status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        // Get authentication details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isDoctor = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR"));
        boolean isPatient = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PATIENT"));
        String email = authentication.getName();

        // Enforce role-based access security
        if (isAdmin) {
            // Admin has full control
        } else if (isDoctor) {
            // Doctor can only update their own appointments
            if (appointment.getDoctor() == null || appointment.getDoctor().getUser() == null ||
                    !appointment.getDoctor().getUser().getEmail().equalsIgnoreCase(email)) {
                throw new BadRequestException("You are not authorized to modify this appointment's status.");
            }
        } else if (isPatient) {
            // Patient can only update their own appointments and ONLY to CANCELLED/REJECTED
            if (appointment.getPatient() == null || appointment.getPatient().getUser() == null ||
                    !appointment.getPatient().getUser().getEmail().equalsIgnoreCase(email)) {
                throw new BadRequestException("You are not authorized to modify this appointment's status.");
            }
            if (!"CANCELLED".equalsIgnoreCase(status) && !"REJECTED".equalsIgnoreCase(status)) {
                throw new BadRequestException("Patients are only permitted to cancel appointments.");
            }
        } else {
            throw new UnauthorizedException("User does not have an authorized role to perform this action");
        }

        AppointmentStatus currentStatus = appointment.getStatus();

        if (currentStatus == AppointmentStatus.EXPIRED) {
            throw new BadRequestException("Cannot update an appointment that has already expired.");
        }

        if ("APPROVED".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status)) {
            if (currentStatus != AppointmentStatus.PENDING) {
                throw new BadRequestException("Only PENDING appointments can be approved. Current status: " + currentStatus);
            }
            appointment.setStatus(AppointmentStatus.CONFIRMED);
        } else if ("REJECTED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            if (currentStatus == AppointmentStatus.COMPLETED) {
                throw new BadRequestException("Cannot cancel an appointment that is already completed.");
            }
            if (currentStatus == AppointmentStatus.NO_SHOW) {
                throw new BadRequestException("Cannot cancel an appointment that is marked as no-show.");
            }
            appointment.setStatus(AppointmentStatus.CANCELLED);
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            if (currentStatus != AppointmentStatus.CONFIRMED) {
                throw new BadRequestException("Only APPROVED appointments can be completed. Current status: " + currentStatus);
            }
            appointment.setStatus(AppointmentStatus.COMPLETED);
        } else if ("NO_SHOW".equalsIgnoreCase(status) || "NO-SHOW".equalsIgnoreCase(status) || "NOSHOW".equalsIgnoreCase(status)) {
            return markAppointmentAsNoShow(id, "Marked as no-show via status update");
        } else {
            throw new BadRequestException("Invalid status update value: " + status);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        if (savedAppointment.getStatus() == AppointmentStatus.CANCELLED) {
            triggerWaitlistCheck(savedAppointment);
        }
        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    public AppointmentResponse markAppointmentAsNoShow(Long appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Get authentication details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isDoctor = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR"));
        String email = authentication.getName();

        // Security check: Only Admin or the assigned Doctor can mark as NO_SHOW
        if (isAdmin) {
            // Authorized
        } else if (isDoctor) {
            if (appointment.getDoctor() == null || appointment.getDoctor().getUser() == null ||
                    !appointment.getDoctor().getUser().getEmail().equalsIgnoreCase(email)) {
                throw new UnauthorizedException("You are not authorized to mark this appointment as no-show.");
            }
        } else {
            throw new BadRequestException("Patients are not authorized to mark appointments as no-show.");
        }

        AppointmentStatus currentStatus = appointment.getStatus();

        // State validation
        if (currentStatus == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot mark a completed appointment as no-show.");
        }
        if (currentStatus == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Cannot mark a cancelled appointment as no-show.");
        }
        if (currentStatus == AppointmentStatus.EXPIRED) {
            throw new BadRequestException("Cannot mark an expired appointment as no-show.");
        }
        if (currentStatus == AppointmentStatus.NO_SHOW) {
            throw new BadRequestException("Appointment is already marked as no-show.");
        }
        if (currentStatus != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED appointments can be marked as no-show. Current status: " + currentStatus);
        }

        // Apply NO_SHOW transition
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        appointment.setNoShowAt(LocalDateTime.now());
        appointment.setNoShowReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Patient did not attend scheduled consultation");
        appointment.setMarkedNoShowBy(email);

        Appointment saved = appointmentRepository.save(appointment);

        // Generate in-app notification for patient
        if (notificationService != null) {
            try {
                notificationService.createNoShowNotification(saved);
            } catch (Exception e) {
                log.warn("Failed to create no-show notification for Appointment #{}: {}", saved.getId(), e.getMessage());
            }
        }

        // Trigger Waitlist automation for the newly freed slot
        triggerWaitlistCheck(saved);

        log.info("Appointment #{} marked as NO_SHOW by {} (Patient #{}, Doctor #{})",
                saved.getId(), email,
                saved.getPatient() != null ? saved.getPatient().getId() : "N/A",
                saved.getDoctor() != null ? saved.getDoctor().getId() : "N/A");

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getNoShowCount(Long doctorId, Long patientId, LocalDate startDate, LocalDate endDate) {
        List<Appointment> noShows = appointmentRepository.findByStatus(AppointmentStatus.NO_SHOW);

        return noShows.stream()
                .filter(a -> doctorId == null || (a.getDoctor() != null && a.getDoctor().getId().equals(doctorId)))
                .filter(a -> patientId == null || (a.getPatient() != null && a.getPatient().getId().equals(patientId)))
                .filter(a -> startDate == null || (a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(startDate)))
                .filter(a -> endDate == null || (a.getAppointmentDate() != null && !a.getAppointmentDate().isAfter(endDate)))
                .count();
    }

    @Override
    public void deleteAppointment(Long id) {
        // Find appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        Doctor doctor = appointment.getDoctor();
        LocalDate date = appointment.getAppointmentDate();
        LocalTime time = appointment.getAppointmentTime();

        // Delete appointment
        appointmentRepository.delete(appointment);

        if (waitlistService != null && doctor != null && date != null && time != null && !date.isBefore(LocalDate.now())) {
            try {
                waitlistService.processWaitlistForSlot(doctor, date, time);
            } catch (Exception e) {
                log.warn("Failed to trigger waitlist on appointment deletion: {}", e.getMessage());
            }
        }
    }

    private void triggerWaitlistCheck(Appointment appointment) {
        if (appointment == null) return;
        Doctor doctor = appointment.getDoctor();
        LocalDate date = appointment.getAppointmentDate();
        LocalTime time = appointment.getAppointmentTime();

        if (waitlistService != null && doctor != null && date != null && time != null && !date.isBefore(LocalDate.now())) {
            try {
                waitlistService.processWaitlistForSlot(doctor, date, time);
            } catch (Exception e) {
                log.warn("Failed to trigger waitlist on appointment cancellation: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public int expireOverdueAppointments() {
        LocalDate today = LocalDate.now();
        List<AppointmentStatus> activeStatuses = Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
        List<Appointment> overdueAppointments = appointmentRepository.findByAppointmentDateBeforeAndStatusIn(today, activeStatuses);

        if (overdueAppointments.isEmpty()) {
            log.debug("No overdue uncompleted appointments found for expiry on: {}", today);
            return 0;
        }

        for (Appointment appointment : overdueAppointments) {
            appointment.setStatus(AppointmentStatus.EXPIRED);
        }

        appointmentRepository.saveAll(overdueAppointments);
        log.info("Automatically expired {} overdue appointment(s) scheduled prior to today ({})", overdueAppointments.size(), today);
        return overdueAppointments.size();
    }

    @Override
    @Transactional
    public int sendAppointmentReminders() {
        return sendAppointmentReminders(LocalDateTime.now());
    }

    @Override
    @Transactional
    public int sendAppointmentReminders(LocalDateTime currentDateTime) {
        if (currentDateTime == null) {
            currentDateTime = LocalDateTime.now();
        }

        // Query CONFIRMED appointments where reminder has not yet been marked as sent
        List<Appointment> candidateAppointments = appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED);

        if (candidateAppointments.isEmpty()) {
            log.debug("No pending confirmed appointments found for reminder processing at {}", currentDateTime);
            return 0;
        }

        int sentCount = 0;
        for (Appointment appointment : candidateAppointments) {
            // Rule: Only CONFIRMED appointments receive reminders
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                continue;
            }

            // Duplicate guard 1: Check reminderSent flag
            if (appointment.isReminderSent()) {
                continue;
            }

            // Guard: Null safety for date/time
            if (appointment.getAppointmentDate() == null || appointment.getAppointmentTime() == null) {
                continue;
            }

            // Combine appointment date and time
            LocalDateTime appointmentDateTime = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime());

            // Calculate duration in minutes until appointment
            long minutesUntilAppointment = ChronoUnit.MINUTES.between(currentDateTime, appointmentDateTime);

            // Rule: Reminder timing is exactly 1 hour (60 minutes) before appointment.
            // When run periodically, a 50 to 65 minute window catches the 1-hour mark,
            // while rejecting < 50m (e.g. 30 mins away) and > 65m (e.g. 75 mins, 2 hours away) and <= 0 (past).
            if (minutesUntilAppointment >= 50 && minutesUntilAppointment <= 65) {
                // Duplicate guard 2: Check existing notification in database
                if (notificationRepository != null && notificationRepository.existsByAppointmentId(appointment.getId())) {
                    appointment.setReminderSent(true);
                    appointmentRepository.save(appointment);
                    continue;
                }

                // Create the in-app notification for the patient
                if (notificationService != null) {
                    notificationService.createAppointmentReminder(appointment);
                }

                // Mark appointment as reminderSent = true to prevent future duplicates
                appointment.setReminderSent(true);
                appointmentRepository.save(appointment);
                sentCount++;
                log.info("Sent 1-hour appointment reminder for Appointment #{} (Patient #{})",
                        appointment.getId(), appointment.getPatient() != null ? appointment.getPatient().getId() : "N/A");
            }
        }

        return sentCount;
    }

    @Override
    @Transactional
    public com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse setAppointmentFollowUp(
            Long appointmentId,
            com.example.healthcareappointmentmanagementsystem.dto.request.FollowUpRequest request,
            String userEmail) {
        if (request == null || request.getFollowUpDate() == null) {
            throw new BadRequestException("Follow-up date is required");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // State check: Cannot set follow-up on cancelled appointment
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Cannot schedule a follow-up for a " + appointment.getStatus() + " appointment.");
        }

        if (request.getFollowUpDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Follow-up date cannot be in the past.");
        }

        // Validate follow-up date is not before consultation date
        if (appointment.getAppointmentDate() != null && request.getFollowUpDate().isBefore(appointment.getAppointmentDate())) {
            throw new BadRequestException("Follow-up date cannot be before consultation date (" + appointment.getAppointmentDate() + ").");
        }

        // Authorization check
        verifyFollowUpModificationAccess(appointment, userEmail);

        // Update appointment follow-up details
        appointment.setFollowUpDate(request.getFollowUpDate());
        appointment.setFollowUpTime(request.getFollowUpTime());
        appointment.setFollowUpNotes(request.getFollowUpNotes());
        appointment.setFollowUpReminderSent(false);
        Appointment saved = appointmentRepository.save(appointment);

        // Synchronize with Prescription if existing
        Long prescriptionId = null;
        if (prescriptionRepository != null) {
            try {
                java.util.Optional<com.example.healthcareappointmentmanagementsystem.entity.Prescription> pxOpt =
                        prescriptionRepository.findByAppointment(saved);
                if (pxOpt.isPresent()) {
                    com.example.healthcareappointmentmanagementsystem.entity.Prescription px = pxOpt.get();
                    px.setNextVisitDate(request.getFollowUpDate());
                    px.setFollowUpTime(request.getFollowUpTime());
                    px.setFollowUpNotes(request.getFollowUpNotes());
                    px.setFollowUpReminderSent(false);
                    com.example.healthcareappointmentmanagementsystem.entity.Prescription savedPx = prescriptionRepository.save(px);
                    prescriptionId = savedPx.getId();
                }
            } catch (Exception e) {
                log.warn("Could not sync follow-up date to prescription: {}", e.getMessage());
            }
        }

        String doctorName = appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                ? "Dr. " + appointment.getDoctor().getUser().getFirstName() + " " + appointment.getDoctor().getUser().getLastName()
                : "Doctor";

        String patientName = appointment.getPatient() != null && appointment.getPatient().getUser() != null
                ? appointment.getPatient().getUser().getFirstName() + " " + appointment.getPatient().getUser().getLastName()
                : "Patient";

        log.info("Follow-up consultation scheduled for Appointment #{} on {} at {} by {}",
                saved.getId(), saved.getFollowUpDate(), saved.getFollowUpTime(), userEmail);

        return com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse.builder()
                .appointmentId(saved.getId())
                .prescriptionId(prescriptionId)
                .patientId(saved.getPatient() != null ? saved.getPatient().getId() : null)
                .patientName(patientName)
                .doctorId(saved.getDoctor() != null ? saved.getDoctor().getId() : null)
                .doctorName(doctorName)
                .followUpDate(saved.getFollowUpDate())
                .followUpTime(saved.getFollowUpTime())
                .followUpNotes(saved.getFollowUpNotes())
                .followUpReminderSent(saved.isFollowUpReminderSent())
                .message("Follow-up consultation successfully scheduled for " + saved.getFollowUpDate() + ".")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse getAppointmentFollowUp(Long appointmentId, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        verifyFollowUpViewAccess(appointment, userEmail);

        String doctorName = appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                ? "Dr. " + appointment.getDoctor().getUser().getFirstName() + " " + appointment.getDoctor().getUser().getLastName()
                : "Doctor";

        String patientName = appointment.getPatient() != null && appointment.getPatient().getUser() != null
                ? appointment.getPatient().getUser().getFirstName() + " " + appointment.getPatient().getUser().getLastName()
                : "Patient";

        Long prescriptionId = null;
        if (prescriptionRepository != null) {
            try {
                java.util.Optional<com.example.healthcareappointmentmanagementsystem.entity.Prescription> pxOpt =
                        prescriptionRepository.findByAppointment(appointment);
                if (pxOpt.isPresent()) {
                    prescriptionId = pxOpt.get().getId();
                }
            } catch (Exception ignored) {}
        }

        return com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse.builder()
                .appointmentId(appointment.getId())
                .prescriptionId(prescriptionId)
                .patientId(appointment.getPatient() != null ? appointment.getPatient().getId() : null)
                .patientName(patientName)
                .doctorId(appointment.getDoctor() != null ? appointment.getDoctor().getId() : null)
                .doctorName(doctorName)
                .followUpDate(appointment.getFollowUpDate())
                .followUpTime(appointment.getFollowUpTime())
                .followUpNotes(appointment.getFollowUpNotes())
                .followUpReminderSent(appointment.isFollowUpReminderSent())
                .message(appointment.getFollowUpDate() != null ? "Follow-up consultation is scheduled." : "No follow-up scheduled.")
                .build();
    }

    @Override
    @Transactional
    public int sendFollowUpReminders() {
        return sendFollowUpReminders(LocalDate.now(), this.followUpDaysBefore);
    }

    @Override
    @Transactional
    public int sendFollowUpReminders(LocalDate referenceDate, int daysBefore) {
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }
        LocalDate targetFollowUpDate = referenceDate.plusDays(daysBefore);

        log.info("Scanning for upcoming follow-up appointments scheduled on {} (advance window: {} day(s))...",
                targetFollowUpDate, daysBefore);

        List<AppointmentStatus> excluded = Collections.singletonList(AppointmentStatus.CANCELLED);
        List<Appointment> candidateAppointments = appointmentRepository.findByFollowUpDateAndStatusNotInAndFollowUpReminderSentFalse(targetFollowUpDate, excluded);

        int sentCount = 0;
        for (Appointment appointment : candidateAppointments) {
            if (appointment.isFollowUpReminderSent()) {
                continue;
            }

            // Exclude cancelled
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }

            // Create notification for patient
            if (notificationService != null) {
                try {
                    notificationService.createFollowUpReminder(appointment);
                } catch (Exception e) {
                    log.warn("Failed to dispatch follow-up reminder for Appointment #{}: {}", appointment.getId(), e.getMessage());
                }
            }

            appointment.setFollowUpReminderSent(true);
            appointmentRepository.save(appointment);

            // Also mark prescription if one exists
            if (prescriptionRepository != null) {
                try {
                    java.util.Optional<com.example.healthcareappointmentmanagementsystem.entity.Prescription> pxOpt =
                            prescriptionRepository.findByAppointment(appointment);
                    if (pxOpt.isPresent()) {
                        com.example.healthcareappointmentmanagementsystem.entity.Prescription px = pxOpt.get();
                        px.setFollowUpReminderSent(true);
                        prescriptionRepository.save(px);
                    }
                } catch (Exception ignored) {}
            }

            sentCount++;
            log.info("Dispatched follow-up reminder for Appointment #{} (Patient #{}, Doctor #{}, FollowUpDate: {})",
                    appointment.getId(),
                    appointment.getPatient() != null ? appointment.getPatient().getId() : "N/A",
                    appointment.getDoctor() != null ? appointment.getDoctor().getId() : "N/A",
                    appointment.getFollowUpDate());
        }

        return sentCount;
    }

    private void verifyFollowUpModificationAccess(Appointment appointment, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return; // Fallback if internal invocation
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return; // Admin has full access
        }

        Doctor doc = appointment.getDoctor();
        if (doc != null && doc.getUser() != null && doc.getUser().getEmail() != null) {
            if (doc.getUser().getEmail().equalsIgnoreCase(userEmail)) {
                return; // Attending doctor
            }
        }

        throw new UnauthorizedException("You are not authorized to schedule or modify follow-up for this appointment.");
    }

    private void verifyFollowUpViewAccess(Appointment appointment, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }

        Patient pat = appointment.getPatient();
        if (pat != null && pat.getUser() != null && pat.getUser().getEmail() != null) {
            if (pat.getUser().getEmail().equalsIgnoreCase(userEmail)) {
                return;
            }
        }

        Doctor doc = appointment.getDoctor();
        if (doc != null && doc.getUser() != null && doc.getUser().getEmail() != null) {
            if (doc.getUser().getEmail().equalsIgnoreCase(userEmail)) {
                return;
            }
        }

        throw new UnauthorizedException("You are not authorized to view follow-up details for this appointment.");
    }
}
