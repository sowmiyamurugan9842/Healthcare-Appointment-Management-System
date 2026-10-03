package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.WaitlistMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.repository.WaitlistRepository;
import com.example.healthcareappointmentmanagementsystem.service.AppointmentService;
import com.example.healthcareappointmentmanagementsystem.service.NotificationService;
import com.example.healthcareappointmentmanagementsystem.service.WaitlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of WaitlistService managing queue registration, slot matching, and offer lifecycles.
 */
@Service
@Transactional
public class WaitlistServiceImpl implements WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistServiceImpl.class);

    private final WaitlistRepository waitlistRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final WaitlistMapper waitlistMapper;

    @Value("${app.waitlist.offer-expiration-minutes:60}")
    private int offerExpirationMinutes = 60;

    public WaitlistServiceImpl(WaitlistRepository waitlistRepository,
                               DoctorRepository doctorRepository,
                               PatientRepository patientRepository,
                               UserRepository userRepository,
                               AppointmentRepository appointmentRepository,
                               AppointmentService appointmentService,
                               NotificationService notificationService,
                               WaitlistMapper waitlistMapper) {
        this.waitlistRepository = waitlistRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
        this.notificationService = notificationService;
        this.waitlistMapper = waitlistMapper;
    }

    @Override
    public WaitlistResponse joinWaitlist(WaitlistRequest request) {
        if (request == null) {
            throw new BadRequestException("Waitlist request cannot be null");
        }

        // 1. Resolve Doctor
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        // 2. Resolve Authenticated Patient
        Patient patient = getAuthenticatedPatient();

        // 3. Validate Date
        LocalDate date = request.getAppointmentDate();
        if (date == null) {
            throw new BadRequestException("Appointment date is required");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Appointment date must be today or in the future");
        }

        // 4. Validate Preferred Time is within Doctor working hours (if specified)
        LocalTime preferredTime = request.getPreferredTime();
        if (preferredTime != null) {
            if (doctor.getAvailableFrom() != null && doctor.getAvailableTo() != null) {
                if (preferredTime.isBefore(doctor.getAvailableFrom()) || preferredTime.isAfter(doctor.getAvailableTo())) {
                    throw new BadRequestException("Preferred time " + preferredTime + " is outside doctor's working hours ("
                            + doctor.getAvailableFrom() + " - " + doctor.getAvailableTo() + ")");
                }
            }
        }

        // 5. Check Duplicate Active Waitlist Entry for same patient, doctor, and date
        boolean alreadyWaiting = waitlistRepository.existsByPatientAndDoctorAndAppointmentDateAndStatusIn(
                patient, doctor, date, Arrays.asList(WaitlistStatus.WAITING, WaitlistStatus.NOTIFIED));
        if (alreadyWaiting) {
            throw new BadRequestException("You are already on the waitlist for this doctor and date.");
        }

        // 6. Check if patient already has an active appointment on the target date
        List<Appointment> existingAppts = appointmentRepository.findByPatientAndAppointmentDate(patient, date);
        boolean hasActiveAppt = existingAppts.stream()
                .anyMatch(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED);
        if (hasActiveAppt) {
            throw new BadRequestException("You already have an active appointment scheduled on " + date);
        }

        // 7. Create and persist WaitlistEntry
        WaitlistEntry entry = waitlistMapper.toEntity(request, doctor, patient);
        entry.setStatus(WaitlistStatus.WAITING);
        WaitlistEntry savedEntry = waitlistRepository.save(entry);

        // 8. Compute Queue Position
        int position = calculateQueuePosition(savedEntry);

        log.info("Patient #{} successfully joined waitlist for Doctor #{} on {} (Position: #{})",
                patient.getId(), doctor.getId(), date, position);

        return waitlistMapper.toResponse(savedEntry, position);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistResponse> getMyWaitlist() {
        Patient patient = getAuthenticatedPatient();
        List<WaitlistEntry> entries = waitlistRepository.findByPatientOrderByCreatedAtDesc(patient);

        return entries.stream()
                .map(e -> {
                    Integer pos = e.getStatus() == WaitlistStatus.WAITING ? calculateQueuePosition(e) : null;
                    return waitlistMapper.toResponse(e, pos);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistResponse> getWaitlistByDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isDoctor = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR"));

        if (!isAdmin && isDoctor) {
            String email = auth.getName();
            if (doctor.getUser() == null || !doctor.getUser().getEmail().equalsIgnoreCase(email)) {
                throw new BadRequestException("You are not authorized to view another doctor's waitlist");
            }
        } else if (!isAdmin && !isDoctor) {
            throw new UnauthorizedException("Access denied: only doctors and administrators can view doctor waitlists");
        }

        List<WaitlistEntry> entries = waitlistRepository.findByDoctorOrderByCreatedAtDesc(doctor);
        return entries.stream()
                .map(e -> {
                    Integer pos = e.getStatus() == WaitlistStatus.WAITING ? calculateQueuePosition(e) : null;
                    return waitlistMapper.toResponse(e, pos);
                })
                .collect(Collectors.toList());
    }

    @Override
    public WaitlistResponse cancelWaitlistEntry(Long id) {
        WaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (!isAdmin) {
            Patient currentPatient = getAuthenticatedPatient();
            if (entry.getPatient() == null || !entry.getPatient().getId().equals(currentPatient.getId())) {
                throw new BadRequestException("You are not authorized to cancel this waitlist entry");
            }
        }

        if (entry.getStatus() == WaitlistStatus.BOOKED) {
            throw new BadRequestException("Cannot cancel a waitlist entry that has already been booked.");
        }
        if (entry.getStatus() == WaitlistStatus.CANCELLED) {
            throw new BadRequestException("Waitlist entry is already cancelled.");
        }
        if (entry.getStatus() == WaitlistStatus.EXPIRED) {
            throw new BadRequestException("Waitlist entry is already expired.");
        }

        boolean wasNotified = (entry.getStatus() == WaitlistStatus.NOTIFIED);
        LocalTime offeredTime = entry.getOfferedTime();
        Doctor doctor = entry.getDoctor();
        LocalDate date = entry.getAppointmentDate();

        entry.setStatus(WaitlistStatus.CANCELLED);
        WaitlistEntry saved = waitlistRepository.save(entry);

        log.info("Waitlist entry #{} cancelled by patient #{}", id, entry.getPatient().getId());

        // If the cancelled entry had an active slot offer, pass the slot opportunity to the next waiting patient
        if (wasNotified && offeredTime != null && doctor != null && date != null && !date.isBefore(LocalDate.now())) {
            processWaitlistForSlot(doctor, date, offeredTime);
        }

        return waitlistMapper.toResponse(saved, null);
    }

    @Override
    public AppointmentResponse confirmOfferedSlot(Long waitlistId) {
        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with ID: " + waitlistId));

        Patient currentPatient = getAuthenticatedPatient();
        if (entry.getPatient() == null || !entry.getPatient().getId().equals(currentPatient.getId())) {
            throw new BadRequestException("You are not authorized to confirm this waitlist offer.");
        }

        if (entry.getStatus() == WaitlistStatus.WAITING) {
            throw new BadRequestException("This waitlist entry is still waiting for an available slot offer.");
        }
        if (entry.getStatus() == WaitlistStatus.BOOKED) {
            throw new BadRequestException("This waitlist entry has already been booked.");
        }
        if (entry.getStatus() == WaitlistStatus.CANCELLED) {
            throw new BadRequestException("This waitlist entry has been cancelled.");
        }
        if (entry.getStatus() == WaitlistStatus.EXPIRED) {
            throw new BadRequestException("The offer for this slot has expired.");
        }

        // Check if offer expired in time
        if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(LocalDateTime.now())) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);

            // Trigger waitlist check for next patient
            if (entry.getDoctor() != null && entry.getAppointmentDate() != null && entry.getOfferedTime() != null) {
                processWaitlistForSlot(entry.getDoctor(), entry.getAppointmentDate(), entry.getOfferedTime());
            }

            throw new BadRequestException("The offer for this slot expired at " + entry.getExpiresAt()
                    + ". The opportunity has been passed to the next patient.");
        }

        LocalTime bookingTime = entry.getOfferedTime() != null ? entry.getOfferedTime() : entry.getPreferredTime();
        if (bookingTime == null) {
            throw new BadRequestException("No confirmed slot time found for this waitlist offer.");
        }

        // Delegate to existing AppointmentService for booking and duplicate-booking safety
        AppointmentRequest appointmentRequest = AppointmentRequest.builder()
                .doctorId(entry.getDoctor().getId())
                .patientId(entry.getPatient().getId())
                .appointmentDate(entry.getAppointmentDate())
                .appointmentTime(bookingTime)
                .reasonForVisit(entry.getReasonForVisit())
                .build();

        AppointmentResponse appointmentResponse = appointmentService.bookAppointment(appointmentRequest);

        // Mark waitlist entry as BOOKED
        entry.setStatus(WaitlistStatus.BOOKED);
        waitlistRepository.save(entry);

        log.info("Waitlist entry #{} confirmed and converted to Appointment #{} for Patient #{}",
                waitlistId, appointmentResponse.getId(), entry.getPatient().getId());

        return appointmentResponse;
    }

    @Override
    public int processWaitlistForSlot(Doctor doctor, LocalDate appointmentDate, LocalTime availableSlot) {
        if (doctor == null || appointmentDate == null || availableSlot == null) {
            return 0;
        }

        // Do not process waitlists for past dates
        if (appointmentDate.isBefore(LocalDate.now())) {
            return 0;
        }

        // Guard: Check if there is already an active (unexpired) NOTIFIED offer for this doctor, date, and slot
        List<WaitlistEntry> notifiedEntries = waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(
                doctor, appointmentDate, WaitlistStatus.NOTIFIED);
        boolean slotAlreadyOffered = notifiedEntries.stream()
                .anyMatch(e -> availableSlot.equals(e.getOfferedTime())
                        && e.getExpiresAt() != null
                        && e.getExpiresAt().isAfter(LocalDateTime.now()));
        if (slotAlreadyOffered) {
            log.debug("Slot {} on {} for Doctor #{} is already held by a pending patient offer.", availableSlot, appointmentDate, doctor.getId());
            return 0;
        }

        // Find all WAITING entries for this doctor and date ordered by createdAt ASC (FIFO fair queue)
        List<WaitlistEntry> waitingEntries = waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(
                doctor, appointmentDate, WaitlistStatus.WAITING);

        if (waitingEntries.isEmpty()) {
            log.debug("No waiting patients found for Doctor #{} on {}", doctor.getId(), appointmentDate);
            return 0;
        }

        // Priority 1: Find earliest patient who explicitly requested this preferred time
        Optional<WaitlistEntry> exactMatch = waitingEntries.stream()
                .filter(e -> e.getPreferredTime() != null && e.getPreferredTime().equals(availableSlot))
                .findFirst();

        WaitlistEntry selectedEntry = null;
        if (exactMatch.isPresent()) {
            selectedEntry = exactMatch.get();
        } else {
            // Priority 2: Find earliest patient who requested "Any available time" (preferredTime == null)
            Optional<WaitlistEntry> anyTimeMatch = waitingEntries.stream()
                    .filter(e -> e.getPreferredTime() == null)
                    .findFirst();
            if (anyTimeMatch.isPresent()) {
                selectedEntry = anyTimeMatch.get();
            }
        }

        if (selectedEntry == null) {
            log.debug("No matching waitlist candidate (exact preferred time or any-time) found for slot {} on {}",
                    availableSlot, appointmentDate);
            return 0;
        }

        // Offer the slot to the selected patient
        int expirationMinutes = this.offerExpirationMinutes > 0 ? this.offerExpirationMinutes : 60;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);

        selectedEntry.setStatus(WaitlistStatus.NOTIFIED);
        selectedEntry.setOfferedTime(availableSlot);
        selectedEntry.setNotifiedAt(now);
        selectedEntry.setExpiresAt(expiresAt);
        waitlistRepository.save(selectedEntry);

        // Send in-app notification to the patient
        notificationService.createWaitlistOfferNotification(
                selectedEntry.getPatient(), doctor, appointmentDate, availableSlot, expirationMinutes);

        log.info("Offered available slot {} on {} to Waitlist Patient #{} (Waitlist Entry #{}, Expires: {})",
                availableSlot, appointmentDate, selectedEntry.getPatient().getId(), selectedEntry.getId(), expiresAt);

        return 1;
    }

    @Override
    public int checkAndExpirePendingOffers() {
        LocalDateTime now = LocalDateTime.now();
        List<WaitlistEntry> expiredOffers = waitlistRepository.findByStatusAndExpiresAtBefore(
                WaitlistStatus.NOTIFIED, now);

        if (expiredOffers.isEmpty()) {
            return 0;
        }

        int reallocatedCount = 0;
        for (WaitlistEntry entry : expiredOffers) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
            log.info("Waitlist offer #{} for Patient #{} expired at {}. Reallocating slot...",
                    entry.getId(), entry.getPatient().getId(), entry.getExpiresAt());

            // Reallocate the expired slot to the next candidate in the queue
            if (entry.getDoctor() != null && entry.getAppointmentDate() != null && entry.getOfferedTime() != null) {
                int matched = processWaitlistForSlot(entry.getDoctor(), entry.getAppointmentDate(), entry.getOfferedTime());
                if (matched > 0) {
                    reallocatedCount++;
                }
            }
        }

        return reallocatedCount;
    }

    private int calculateQueuePosition(WaitlistEntry entry) {
        if (entry == null || entry.getDoctor() == null || entry.getAppointmentDate() == null || entry.getCreatedAt() == null) {
            return 1;
        }
        long earlierCount = waitlistRepository.countByDoctorAndAppointmentDateAndStatusAndCreatedAtBefore(
                entry.getDoctor(), entry.getAppointmentDate(), WaitlistStatus.WAITING, entry.getCreatedAt());
        return (int) earlierCount + 1;
    }

    private Patient getAuthenticatedPatient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UnauthorizedException("User is not authenticated. Please log in.");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User record not found for email: " + email));

        return patientRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + email));
    }
}
