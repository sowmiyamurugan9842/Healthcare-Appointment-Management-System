package com.example.healthcareappointmentmanagementsystem.service.impl;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.request.ChatbotRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.ChatbotResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.DepartmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorAvailableSlotsResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Department;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.ResourceNotFoundException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.mapper.DoctorMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DepartmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.service.AIService;
import com.example.healthcareappointmentmanagementsystem.service.AppointmentService;
import com.example.healthcareappointmentmanagementsystem.service.ChatbotService;
import com.example.healthcareappointmentmanagementsystem.service.DoctorService;
import com.example.healthcareappointmentmanagementsystem.service.PrescriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of ChatbotService providing secure, tool-augmented AI healthcare assistance.
 * Routes user queries to controlled backend APIs and protects patient data.
 */
@Service
public class ChatbotServiceImpl implements ChatbotService {


    private static final Logger log = LoggerFactory.getLogger(ChatbotServiceImpl.class);

    private final DoctorService doctorService;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PrescriptionService prescriptionService;
    private final com.example.healthcareappointmentmanagementsystem.service.WaitlistService waitlistService;
    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final AIService aiService;

    @org.springframework.beans.factory.annotation.Autowired
    public ChatbotServiceImpl(DoctorService doctorService,
                              DoctorRepository doctorRepository,
                              DepartmentRepository departmentRepository,
                              AppointmentService appointmentService,
                              AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              UserRepository userRepository,
                              PrescriptionService prescriptionService,
                              @org.springframework.context.annotation.Lazy com.example.healthcareappointmentmanagementsystem.service.WaitlistService waitlistService,
                              AppointmentMapper appointmentMapper,
                              DoctorMapper doctorMapper,
                              AIService aiService) {
        this.doctorService = doctorService;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentService = appointmentService;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.prescriptionService = prescriptionService;
        this.waitlistService = waitlistService;
        this.appointmentMapper = appointmentMapper;
        this.doctorMapper = doctorMapper;
        this.aiService = aiService;
    }

    /**
     * Backward-compatible 11-argument constructor for legacy test harnesses.
     */
    public ChatbotServiceImpl(DoctorService doctorService,
                              DoctorRepository doctorRepository,
                              DepartmentRepository departmentRepository,
                              AppointmentService appointmentService,
                              AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              UserRepository userRepository,
                              PrescriptionService prescriptionService,
                              AppointmentMapper appointmentMapper,
                              DoctorMapper doctorMapper,
                              AIService aiService) {
        this(doctorService, doctorRepository, departmentRepository, appointmentService, appointmentRepository,
                patientRepository, userRepository, prescriptionService, null, appointmentMapper, doctorMapper, aiService);
    }

    @Override
    public ChatbotResponse processMessage(ChatbotRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new BadRequestException("Message cannot be empty");
        }

        String rawMsg = request.getMessage().trim();
        String lowerMsg = rawMsg.toLowerCase(Locale.ROOT);

        // 1. SAFETY RULE: Never diagnose or provide medical prescriptions
        if (isMedicalDiagnosisOrPrescriptionQuery(lowerMsg)) {
            return ChatbotResponse.builder()
                    .replyType("SAFETY_WARNING")
                    .message("⚠️ **Medical Notice**: I am your CarePortal AI administrative assistant. I can assist you with scheduling appointments, searching for physicians, finding open consultation slots, and answering hospital system questions.\n\n"
                            + "However, I **cannot provide medical diagnoses, evaluate clinical symptoms, or prescribe medication**.\n\n"
                            + "For your safety, please consult a licensed healthcare professional or contact emergency medical services if you need medical evaluation.")
                    .build();
        }

        // 2. Resolve Authenticated User and Patient Profile
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UnauthorizedException("User is not authenticated. Please log in to use CarePortal Assistant.");
        }

        String userEmail = auth.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User record not found for email: " + userEmail));
        Patient patient = patientRepository.findByUser(user).orElse(null);

        // 3. Check for multi-turn booking or waitlist state
        String state = request.getConversationState();
        if ("AWAITING_WAITLIST_CONFIRMATION".equalsIgnoreCase(state)) {
            return handleWaitlistOfferConfirmation(request, patient, lowerMsg);
        }

        if ("AWAITING_CONFIRMATION".equalsIgnoreCase(state)) {
            return handleBookingConfirmation(request, patient, lowerMsg);
        }

        // 4. Intent Classification and Tool Dispatch
        if (isConfirmWaitlistOfferIntent(lowerMsg) || request.getWaitlistIdToConfirm() != null) {
            return handleConfirmWaitlistOffer(request, patient, lowerMsg);
        }

        if (isCancelWaitlistIntent(lowerMsg) || request.getWaitlistIdToCancel() != null) {
            return handleCancelWaitlist(request, patient, lowerMsg);
        }

        if (isMyWaitlistQuery(lowerMsg)) {
            return handleMyWaitlistQuery(patient);
        }

        if (isNoShowQuery(lowerMsg)) {
            return handleNoShowInquiry(request, patient, user, lowerMsg);
        }

        if (isJoinWaitlistIntent(lowerMsg)) {
            return handleJoinWaitlistDirect(request, patient, lowerMsg);
        }

        if (isCancelAppointmentIntent(lowerMsg)) {
            return handleAppointmentCancellation(request, patient, lowerMsg);
        }

        if (isBookingIntent(lowerMsg) || request.getSelectedDoctorId() != null || "AWAITING_SLOT".equalsIgnoreCase(state)) {
            return handleGuidedBookingFlow(request, patient, lowerMsg);
        }

        if (isAvailableSlotsQuery(lowerMsg)) {
            return handleAvailableSlotsQuery(request, lowerMsg);
        }

        if (isMyAppointmentsQuery(lowerMsg)) {
            return handleMyAppointmentsQuery(patient);
        }

        if (isDoctorSearchQuery(lowerMsg)) {
            return handleDoctorSearch(lowerMsg);
        }

        if (isDepartmentQuery(lowerMsg)) {
            return handleDepartmentQuery();
        }

        if (isFollowUpQuery(lowerMsg)) {
            return handleFollowUpQuery(patient);
        }

        if (isPrescriptionQuery(lowerMsg)) {
            return handlePrescriptionQuery(patient);
        }

        if (isGreetingOrHelp(lowerMsg)) {
            return handleGreetingOrHelp(user);
        }

        // 5. Default General Assistance
        return handleGeneralSystemInquiry(rawMsg);
    }

    // =========================================================================
    // SAFETY & MEDICAL FILTER
    // =========================================================================
    private boolean isMedicalDiagnosisOrPrescriptionQuery(String msg) {
        String[] medicalKeywords = {
                "diagnose", "diagnosis", "cure my", "treatment for", "what disease do i have",
                "prescribe", "write prescription", "what medicine should i take",
                "dosage of", "how many mg of", "chest pain", "heart attack", "stroke symptoms",
                "is this cancer", "am i having a stroke"
        };
        for (String kw : medicalKeywords) {
            if (msg.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // 1. DOCTOR SEARCH & INFORMATION
    // =========================================================================
    private boolean isDoctorSearchQuery(String msg) {
        return msg.contains("doctor") || msg.contains("cardiologist") || msg.contains("neurologist")
                || msg.contains("dermatologist") || msg.contains("pediatrician") || msg.contains("orthopedic")
                || msg.contains("physician") || msg.contains("specialist") || msg.contains("who is available")
                || msg.contains("find a doctor") || msg.contains("show doctors");
    }

    private ChatbotResponse handleDoctorSearch(String msg) {
        List<DoctorResponse> allDoctors = doctorService.getAllDoctors();

        // Check for specific specialization keywords
        String[] specializations = {"cardiology", "cardiologist", "neurology", "neurologist",
                "dermatology", "dermatologist", "orthopedics", "orthopedic", "pediatrics", "pediatrician",
                "general", "internal medicine"};

        String detectedSpec = null;
        for (String s : specializations) {
            if (msg.contains(s)) {
                detectedSpec = s;
                break;
            }
        }

        List<DoctorResponse> filtered = allDoctors;
        if (detectedSpec != null) {
            String specClean = detectedSpec.replace("ist", "y").replace("ician", "ics");
            filtered = allDoctors.stream()
                    .filter(d -> (d.getSpecialization() != null && d.getSpecialization().toLowerCase().contains(specClean.substring(0, Math.min(4, specClean.length()))))
                            || (d.getDepartmentName() != null && d.getDepartmentName().toLowerCase().contains(specClean.substring(0, Math.min(4, specClean.length())))))
                    .collect(Collectors.toList());
        }

        if (filtered.isEmpty()) {
            filtered = allDoctors;
        }

        StringBuilder sb = new StringBuilder();
        if (detectedSpec != null) {
            sb.append("Here are our specialists matching **").append(detectedSpec).append("**:\n\n");
        } else {
            sb.append("Here are our available consulting physicians at CarePortal:\n\n");
        }

        for (DoctorResponse doc : filtered) {
            sb.append("👨‍⚕️ **Dr. ").append(doc.getFullName()).append("**\n")
                    .append("• **Specialization**: ").append(doc.getSpecialization()).append(" (").append(doc.getDepartmentName()).append(")\n")
                    .append("• **Qualifications**: ").append(doc.getQualification()).append(" (").append(doc.getExperienceYears()).append(" yrs exp)\n")
                    .append("• **Consultation Fee**: $").append(String.format("%.2f", doc.getConsultationFee())).append("\n")
                    .append("• **Working Hours**: ").append(doc.getAvailableFrom()).append(" - ").append(doc.getAvailableTo()).append("\n\n");
        }

        sb.append("To check open appointment slots, simply choose a doctor and tell me your preferred date (e.g., *'Available slots for Dr. ").append(filtered.get(0).getFullName()).append(" tomorrow'*).");

        return ChatbotResponse.builder()
                .replyType("DOCTOR_LIST")
                .message(sb.toString())
                .doctors(filtered)
                .nextAction("SELECT_DOCTOR")
                .build();
    }

    // =========================================================================
    // 2. AVAILABLE APPOINTMENT SLOTS
    // =========================================================================
    private boolean isAvailableSlotsQuery(String msg) {
        return msg.contains("slot") || msg.contains("available time") || msg.contains("open time")
                || msg.contains("available tomorrow") || msg.contains("is 10 am available") || msg.contains("when is");
    }

    private ChatbotResponse handleAvailableSlotsQuery(ChatbotRequest request, String msg) {
        Long docId = request.getSelectedDoctorId();
        List<DoctorResponse> doctors = doctorService.getAllDoctors();

        if (docId == null) {
            // Attempt to match doctor by name mentioned in message
            for (DoctorResponse d : doctors) {
                if (msg.contains(d.getFullName().toLowerCase())) {
                    docId = d.getId();
                    break;
                }
            }
        }

        if (docId == null && !doctors.isEmpty()) {
            docId = doctors.get(0).getId();
        }

        if (docId == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("Please specify which doctor you would like to view available slots for.")
                    .doctors(doctors)
                    .nextAction("SELECT_DOCTOR")
                    .build();
        }

        DoctorResponse doc = doctorService.getDoctorById(docId);
        LocalDate targetDate = parseDateFromMessage(msg, request.getSelectedDate());

        try {
            DoctorAvailableSlotsResponse slotsResponse = doctorService.getAvailableSlots(docId, targetDate);
            List<String> slots = slotsResponse.getAvailableSlots();

            if (slots.isEmpty()) {
                Map<String, Object> waitlistCtx = new HashMap<>();
                waitlistCtx.put("doctorId", docId);
                waitlistCtx.put("doctorName", doc.getFullName());
                waitlistCtx.put("date", targetDate.toString());

                return ChatbotResponse.builder()
                        .replyType("WAITLIST_OFFER_PROMPT")
                        .message("⚠️ All appointment slots for **Dr. " + doc.getFullName() + "** on **" + targetDate + "** are currently booked.\n\nWould you like to join the waitlist?")
                        .doctorId(docId)
                        .doctorName(doc.getFullName())
                        .departmentName(doc.getDepartmentName())
                        .appointmentDate(targetDate.toString())
                        .availableSlots(Collections.emptyList())
                        .conversationState("AWAITING_WAITLIST_CONFIRMATION")
                        .nextAction("OFFER_WAITLIST")
                        .context(waitlistCtx)
                        .build();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📅 Available 30-minute consultation slots for **Dr. ").append(doc.getFullName())
                    .append("** on **").append(targetDate).append("**:\n\n");

            for (String slot : slots) {
                sb.append("• ").append(slot).append("\n");
            }
            sb.append("\nClick any slot below or type the time you would like to reserve!");

            Map<String, Object> ctx = new HashMap<>();
            ctx.put("doctorId", docId);
            ctx.put("doctorName", doc.getFullName());
            ctx.put("date", targetDate.toString());

            return ChatbotResponse.builder()
                    .replyType("SLOTS_LIST")
                    .message(sb.toString())
                    .doctorId(docId)
                    .doctorName(doc.getFullName())
                    .departmentName(doc.getDepartmentName())
                    .appointmentDate(targetDate.toString())
                    .availableSlots(slots)
                    .conversationState("AWAITING_SLOT")
                    .nextAction("SELECT_SLOT")
                    .context(ctx)
                    .build();
        } catch (BadRequestException ex) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ " + ex.getMessage() + ". Please choose a date today or in the future.")
                    .doctorId(docId)
                    .build();
        }
    }

    // =========================================================================
    // 3. APPOINTMENT INFORMATION
    // =========================================================================
    private boolean isMyAppointmentsQuery(String msg) {
        return msg.contains("my appointment") || msg.contains("upcoming appointment")
                || msg.contains("do i have an appointment") || msg.contains("check my appointment")
                || msg.contains("appointment status") || msg.contains("when is my next appointment");
    }

    private ChatbotResponse handleMyAppointmentsQuery(Patient patient) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("You do not have a patient medical profile registered yet. Please register your patient profile to view appointments.")
                    .build();
        }

        List<Appointment> appts = appointmentRepository.findByPatient(patient);
        if (appts.isEmpty()) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("You currently have no scheduled appointments. Would you like me to help you book one?")
                    .nextAction("SELECT_DOCTOR")
                    .build();
        }

        // Sort appointments chronologically
        List<AppointmentResponse> sortedAppts = appts.stream()
                .map(appointmentMapper::toResponse)
                .sorted(Comparator.comparing(AppointmentResponse::getAppointmentDate)
                        .thenComparing(AppointmentResponse::getAppointmentTime))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("📋 **Your Scheduled Appointments**:\n\n");

        for (AppointmentResponse a : sortedAppts) {
            String statusEmoji = switch (a.getStatus()) {
                case CONFIRMED -> "✅";
                case PENDING -> "⏳";
                case COMPLETED -> "🩺";
                case CANCELLED -> "❌";
                case EXPIRED -> "⏰";
                case NO_SHOW -> "⚠️";
                default -> "ℹ️";
            };

            sb.append("• **Appointment #").append(a.getId()).append("** ").append(statusEmoji).append(" [").append(a.getStatus()).append("]\n")
                    .append("   Doctor: Dr. ").append(a.getDoctorName()).append(" (").append(a.getDepartmentName()).append(")\n")
                    .append("   Date & Time: ").append(a.getAppointmentDate()).append(" at ").append(a.getAppointmentTime()).append("\n")
                    .append("   Reason: ").append(a.getReasonForVisit()).append("\n\n");
        }

        return ChatbotResponse.builder()
                .replyType("APPOINTMENT_LIST")
                .message(sb.toString())
                .appointments(sortedAppts)
                .build();
    }

    // =========================================================================
    // 4. GUIDED BOOKING ASSISTANCE & MULTI-TURN FLOW
    // =========================================================================
    private boolean isBookingIntent(String msg) {
        return msg.contains("book") || msg.contains("schedule") || msg.contains("make an appointment")
                || msg.contains("reserve") || msg.contains("consultation with");
    }

    private ChatbotResponse handleGuidedBookingFlow(ChatbotRequest request, Patient patient, String msg) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ To book an appointment, you must have an active patient profile. Please complete your patient profile setup.")
                    .build();
        }

        List<DoctorResponse> doctors = doctorService.getAllDoctors();
        Long doctorId = request.getSelectedDoctorId();

        // 1. Identify Doctor
        if (doctorId == null) {
            for (DoctorResponse d : doctors) {
                if (msg.contains(d.getFullName().toLowerCase())) {
                    doctorId = d.getId();
                    break;
                }
            }
        }

        if (doctorId == null) {
            return ChatbotResponse.builder()
                    .replyType("DOCTOR_LIST")
                    .message("I would be happy to help you book an appointment! Which doctor or medical department would you like to consult?")
                    .doctors(doctors)
                    .nextAction("SELECT_DOCTOR")
                    .build();
        }

        DoctorResponse doc = doctorService.getDoctorById(doctorId);

        // 2. Identify Date
        LocalDate targetDate = parseDateFromMessage(msg, request.getSelectedDate());

        // 3. Identify Time Slot
        String chosenTime = request.getSelectedTime();
        if (chosenTime == null || chosenTime.isBlank()) {
            chosenTime = parseTimeFromMessage(msg);
        }

        // If time is not specified yet, fetch open slots and ask user to choose
        if (chosenTime == null || chosenTime.isBlank()) {
            try {
                DoctorAvailableSlotsResponse slotsRes = doctorService.getAvailableSlots(doctorId, targetDate);
                List<String> available = slotsRes.getAvailableSlots();

                if (available.isEmpty()) {
                    Map<String, Object> waitlistCtx = new HashMap<>();
                    waitlistCtx.put("doctorId", doctorId);
                    waitlistCtx.put("doctorName", doc.getFullName());
                    waitlistCtx.put("date", targetDate.toString());

                    return ChatbotResponse.builder()
                            .replyType("WAITLIST_OFFER_PROMPT")
                            .message("⚠️ All appointment slots for **Dr. " + doc.getFullName() + "** on **" + targetDate + "** are currently booked.\n\nWould you like to join the waitlist?")
                            .doctorId(doctorId)
                            .doctorName(doc.getFullName())
                            .departmentName(doc.getDepartmentName())
                            .appointmentDate(targetDate.toString())
                            .availableSlots(Collections.emptyList())
                            .conversationState("AWAITING_WAITLIST_CONFIRMATION")
                            .nextAction("OFFER_WAITLIST")
                            .context(waitlistCtx)
                            .build();
                }

                Map<String, Object> ctx = new HashMap<>();
                ctx.put("doctorId", doctorId);
                ctx.put("doctorName", doc.getFullName());
                ctx.put("date", targetDate.toString());

                return ChatbotResponse.builder()
                        .replyType("SLOTS_LIST")
                        .message("Great! Here are the available consultation slots for **Dr. " + doc.getFullName()
                                + "** on **" + targetDate + "**:\n\nPlease select your preferred slot:")
                        .doctorId(doctorId)
                        .doctorName(doc.getFullName())
                        .departmentName(doc.getDepartmentName())
                        .appointmentDate(targetDate.toString())
                        .availableSlots(available)
                        .conversationState("AWAITING_SLOT")
                        .nextAction("SELECT_SLOT")
                        .context(ctx)
                        .build();
            } catch (BadRequestException ex) {
                return ChatbotResponse.builder()
                        .replyType("TEXT")
                        .message("⚠️ " + ex.getMessage())
                        .doctorId(doctorId)
                        .build();
            }
        }

        // 4. Time slot is selected -> Generate Confirmation Prompt Card
        String reason = (request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : "General medical consultation and evaluation";

        String formattedTime = chosenTime.length() == 5 ? chosenTime + ":00" : chosenTime;

        Map<String, Object> bookingCtx = new HashMap<>();
        bookingCtx.put("doctorId", doctorId);
        bookingCtx.put("doctorName", doc.getFullName());
        bookingCtx.put("date", targetDate.toString());
        bookingCtx.put("time", formattedTime);
        bookingCtx.put("reason", reason);
        bookingCtx.put("fee", doc.getConsultationFee());

        String confirmationMsg = "📋 **Please review your appointment booking details**:\n\n"
                + "• **Doctor**: Dr. " + doc.getFullName() + " (" + doc.getSpecialization() + ")\n"
                + "• **Department**: " + doc.getDepartmentName() + "\n"
                + "• **Date**: " + targetDate + "\n"
                + "• **Time**: " + chosenTime.substring(0, 5) + "\n"
                + "• **Consultation Fee**: $" + String.format("%.2f", doc.getConsultationFee()) + "\n"
                + "• **Reason**: " + reason + "\n\n"
                + "Would you like to confirm and schedule this appointment?";

        return ChatbotResponse.builder()
                .replyType("CONFIRMATION_PROMPT")
                .message(confirmationMsg)
                .doctorId(doctorId)
                .doctorName(doc.getFullName())
                .departmentName(doc.getDepartmentName())
                .appointmentDate(targetDate.toString())
                .appointmentTime(chosenTime)
                .reason(reason)
                .conversationState("AWAITING_CONFIRMATION")
                .nextAction("CONFIRM_BOOKING")
                .context(bookingCtx)
                .build();
    }

    private ChatbotResponse handleBookingConfirmation(ChatbotRequest request, Patient patient, String msg) {
        if (msg.contains("no") || msg.contains("cancel") || msg.contains("abort") || msg.contains("stop")) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("Appointment booking has been cancelled. Let me know if you would like to search for other doctors or dates!")
                    .conversationState("NONE")
                    .build();
        }

        Map<String, Object> ctx = request.getContext();
        Long doctorId = request.getSelectedDoctorId();
        String dateStr = request.getSelectedDate();
        String timeStr = request.getSelectedTime();
        String reason = request.getReason();

        if (ctx != null) {
            if (doctorId == null && ctx.get("doctorId") != null) {
                doctorId = Long.valueOf(ctx.get("doctorId").toString());
            }
            if (dateStr == null && ctx.get("date") != null) {
                dateStr = ctx.get("date").toString();
            }
            if (timeStr == null && ctx.get("time") != null) {
                timeStr = ctx.get("time").toString();
            }
            if (reason == null && ctx.get("reason") != null) {
                reason = ctx.get("reason").toString();
            }
        }

        if (doctorId == null || dateStr == null || timeStr == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("Missing booking details. Let's start over: Which doctor would you like to consult?")
                    .nextAction("SELECT_DOCTOR")
                    .conversationState("NONE")
                    .build();
        }

        if (reason == null || reason.length() < 10) {
            reason = "General consultation and medical follow-up";
        }

        LocalDate date = LocalDate.parse(dateStr);
        LocalTime time = LocalTime.parse(timeStr.length() == 5 ? timeStr + ":00" : timeStr);

        try {
            AppointmentRequest apptReq = AppointmentRequest.builder()
                    .doctorId(doctorId)
                    .patientId(patient.getId())
                    .appointmentDate(date)
                    .appointmentTime(time)
                    .reasonForVisit(reason)
                    .build();

            AppointmentResponse saved = appointmentService.bookAppointment(apptReq);

            String successMsg = "🎉 **Appointment Confirmed Successfully!**\n\n"
                    + "• **Appointment ID**: #" + saved.getId() + "\n"
                    + "• **Doctor**: Dr. " + saved.getDoctorName() + "\n"
                    + "• **Date**: " + saved.getAppointmentDate() + "\n"
                    + "• **Time**: " + saved.getAppointmentTime() + "\n"
                    + "• **Status**: " + saved.getStatus() + "\n\n"
                    + "Your appointment has been registered in the system. You will receive an automatic reminder 1 hour before your visit.";

            return ChatbotResponse.builder()
                    .replyType("BOOKING_SUCCESS")
                    .message(successMsg)
                    .appointments(List.of(saved))
                    .conversationState("NONE")
                    .build();
        } catch (BadRequestException | ResourceNotFoundException ex) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ Booking failed: " + ex.getMessage() + ". Please choose another time slot.")
                    .conversationState("NONE")
                    .nextAction("SELECT_SLOT")
                    .build();
        }
    }

    // =========================================================================
    // 5. APPOINTMENT CANCELLATION
    // =========================================================================
    private boolean isCancelAppointmentIntent(String msg) {
        return msg.contains("cancel appointment") || msg.contains("cancel my appointment")
                || msg.contains("cancel booking") || msg.contains("cancel visit");
    }

    private ChatbotResponse handleAppointmentCancellation(ChatbotRequest request, Patient patient, String msg) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("No patient profile found. You must be logged in as a registered patient to cancel appointments.")
                    .build();
        }

        Long apptId = request.getAppointmentIdToCancel();
        if (apptId == null) {
            // Extract number from message (e.g. "cancel appointment 25" or "#25")
            Pattern p = Pattern.compile("(?:#|appointment\\s+|id\\s+)?(\\d+)");
            Matcher m = p.matcher(msg);
            if (m.find()) {
                try {
                    apptId = Long.parseLong(m.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (apptId == null) {
            List<Appointment> appts = appointmentRepository.findByPatientAndStatus(patient, AppointmentStatus.CONFIRMED);
            List<Appointment> pending = appointmentRepository.findByPatientAndStatus(patient, AppointmentStatus.PENDING);
            appts.addAll(pending);

            if (appts.isEmpty()) {
                return ChatbotResponse.builder()
                        .replyType("TEXT")
                        .message("You currently have no active or pending appointments to cancel.")
                        .build();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Which appointment would you like to cancel? Please specify the appointment number:\n\n");
            for (Appointment a : appts) {
                sb.append("• **Appointment #").append(a.getId()).append("**: Dr. ").append(a.getDoctor().getUser().getFirstName()).append(" ").append(a.getDoctor().getUser().getLastName())
                        .append(" on ").append(a.getAppointmentDate()).append(" at ").append(a.getAppointmentTime()).append("\n");
            }

            return ChatbotResponse.builder()
                    .replyType("APPOINTMENT_LIST")
                    .message(sb.toString())
                    .appointments(appts.stream().map(appointmentMapper::toResponse).collect(Collectors.toList()))
                    .nextAction("CONFIRM_CANCEL")
                    .build();
        }

        try {
            AppointmentResponse cancelled = appointmentService.cancelAppointment(apptId);
            return ChatbotResponse.builder()
                    .replyType("CANCELLATION_SUCCESS")
                    .message("✓ **Appointment #" + apptId + " has been successfully cancelled**.\n\nYour slot with Dr. " + cancelled.getDoctorName() + " on " + cancelled.getAppointmentDate() + " has been released.")
                    .appointments(List.of(cancelled))
                    .build();
        } catch (Exception ex) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ Could not cancel appointment: " + ex.getMessage())
                    .build();
        }
    }

    // =========================================================================
    // 6. DEPARTMENT INFORMATION
    // =========================================================================
    private boolean isDepartmentQuery(String msg) {
        return msg.contains("department") || msg.contains("specialties") || msg.contains("what services")
                || msg.contains("hospital branches") || msg.contains("clinical departments");
    }

    private ChatbotResponse handleDepartmentQuery() {
        List<Department> depts = departmentRepository.findAll();
        List<DepartmentResponse> resList = depts.stream()
                .map(d -> DepartmentResponse.builder()
                        .id(d.getId())
                        .departmentName(d.getDepartmentName())
                        .description(d.getDescription())
                        .build())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("🏥 **CarePortal Medical Departments**:\n\n");

        for (DepartmentResponse d : resList) {
            sb.append("• **").append(d.getDepartmentName()).append("**\n")
                    .append("   ").append(d.getDescription() != null ? d.getDescription() : "Comprehensive clinical specialist services").append("\n\n");
        }

        sb.append("Would you like to view doctors available in any of these departments?");

        return ChatbotResponse.builder()
                .replyType("TEXT")
                .message(sb.toString())
                .departments(resList)
                .nextAction("SELECT_DOCTOR")
                .build();
    }

    // =========================================================================
    // 7. PRESCRIPTION INFORMATION
    // =========================================================================
    private boolean isPrescriptionQuery(String msg) {
        return msg.contains("prescription") || msg.contains("medication") || msg.contains("my rx")
                || msg.contains("medicines") || msg.contains("prescribed");
    }

    private ChatbotResponse handlePrescriptionQuery(Patient patient) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("No patient profile found. Please register as a patient to view your medical prescriptions.")
                    .build();
        }

        List<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsByPatient(patient.getId());
        if (prescriptions.isEmpty()) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("You do not have any digital prescriptions on record. Prescriptions are issued by doctors after completed consultations.")
                    .build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("💊 **Your Medical Prescriptions on Record**:\n\n");

        for (PrescriptionResponse p : prescriptions) {
            sb.append("• **Prescription #").append(p.getId()).append("** (Issued by Dr. ").append(p.getDoctorName()).append(")\n")
                    .append("   **Diagnosis**: ").append(p.getDiagnosis()).append("\n");

            if (p.getMedicines() != null && !p.getMedicines().isEmpty()) {
                sb.append("   **Medications**:\n");
                for (var m : p.getMedicines()) {
                    sb.append("     - ").append(m.getMedicineName()).append(" (").append(m.getDosage()).append("): ")
                            .append(m.getFrequency()).append(" for ").append(m.getDuration()).append("\n");
                }
            } else if (p.getMedications() != null) {
                sb.append("   **Medications**: ").append(p.getMedications()).append("\n");
            }

            if (p.getDoctorAdvice() != null && !p.getDoctorAdvice().isBlank()) {
                sb.append("   **Doctor's Advice**: ").append(p.getDoctorAdvice()).append("\n");
            }
            if (p.getNextVisitDate() != null) {
                sb.append("   **Follow-up Date**: ").append(p.getNextVisitDate()).append("\n");
            }
            sb.append("\n");
        }

        return ChatbotResponse.builder()
                .replyType("PRESCRIPTION_LIST")
                .message(sb.toString())
                .prescriptions(prescriptions)
                .build();
    }

    // =========================================================================
    // 8. FOLLOW-UP CONSULTATION INQUIRIES
    // =========================================================================
    private boolean isFollowUpQuery(String msg) {
        return msg.contains("follow-up") || msg.contains("follow up") || msg.contains("followup")
                || msg.contains("next visit") || msg.contains("next consultation")
                || msg.contains("when should i visit again") || msg.contains("when to see doctor again");
    }

    private ChatbotResponse handleFollowUpQuery(Patient patient) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("No patient profile found. Please link or register as a patient to view your scheduled follow-up consultations.")
                    .build();
        }

        List<Appointment> appts = appointmentRepository.findByPatient(patient);
        List<Appointment> followUpAppts = appts.stream()
                .filter(a -> a.getFollowUpDate() != null && a.getStatus() != AppointmentStatus.CANCELLED)
                .sorted(Comparator.comparing(Appointment::getFollowUpDate))
                .collect(Collectors.toList());

        if (followUpAppts.isEmpty()) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("📅 You currently do not have any upcoming follow-up consultations scheduled.\n\n"
                            + "When your doctor specifies a follow-up date in your digital prescription, you will receive an automatic reminder 1 day before.")
                    .build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📅 **Your Scheduled Follow-up Consultations**:\n\n");

        for (Appointment a : followUpAppts) {
            String docName = a.getDoctor() != null && a.getDoctor().getUser() != null
                    ? "Dr. " + a.getDoctor().getUser().getFirstName() + " " + a.getDoctor().getUser().getLastName()
                    : "Attending Doctor";

            String formattedDate = a.getFollowUpDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            String timeStr = a.getFollowUpTime() != null ? " at " + a.getFollowUpTime().format(DateTimeFormatter.ofPattern("hh:mm a")) : "";

            sb.append("• **").append(docName).append("**\n")
                    .append("   📅 **Date**: ").append(formattedDate).append(timeStr).append("\n");

            if (a.getFollowUpNotes() != null && !a.getFollowUpNotes().isBlank()) {
                sb.append("   📝 **Instructions**: ").append(a.getFollowUpNotes()).append("\n");
            }
            sb.append("   🔗 Associated with Appointment #").append(a.getId()).append("\n\n");
        }

        sb.append("💡 *Note: CarePortal automatically delivers a notification reminder 1 day prior to each follow-up date.*");

        return ChatbotResponse.builder()
                .replyType("TEXT")
                .message(sb.toString())
                .build();
    }

    // =========================================================================
    // 8. GREETINGS & SYSTEM INQUIRIES
    // =========================================================================
    private boolean isGreetingOrHelp(String msg) {
        return msg.equals("hi") || msg.equals("hello") || msg.equals("hey") || msg.equals("help")
                || msg.startsWith("hi ") || msg.startsWith("hello ") || msg.contains("how can you help")
                || msg.contains("what can you do");
    }

    private ChatbotResponse handleGreetingOrHelp(User user) {
        String name = user != null ? user.getFirstName() : "there";
        String welcome = "👋 Hello " + name + "! I am your **CarePortal AI Assistant**.\n\n"
                + "Here is how I can assist you today:\n"
                + "1. 🔍 **Find Doctors**: Ask *'Show cardiology doctors'* or *'Find a specialist'*\n"
                + "2. 🕒 **Check Open Slots**: Ask *'Available slots for Dr. Smith tomorrow'*\n"
                + "3. 📅 **Book Appointments**: Say *'Book an appointment'* for guided booking\n"
                + "4. 📋 **View Appointments**: Ask *'Show my upcoming appointments'*\n"
                + "5. ❌ **Cancel Bookings**: Say *'Cancel my appointment'*\n"
                + "6. 💊 **View Prescriptions**: Ask *'Show my medications'*\n"
                + "7. 🏥 **Hospital Info**: Ask *'What departments are available?'*\n\n"
                + "How may I help you right now?";

        return ChatbotResponse.builder()
                .replyType("TEXT")
                .message(welcome)
                .build();
    }

    private ChatbotResponse handleGeneralSystemInquiry(String rawMsg) {
        String systemPrompt = "You are CarePortal AI, a helpful, polite hospital assistant. Answer user inquiries about healthcare navigation, appointments, and hospital features clearly without medical diagnosis.";
        String response = aiService.generateResponse(systemPrompt,
                "I am here to help you manage your healthcare appointments, locate consulting physicians, check open slots, and access your medical records. Please let me know what you would like assistance with!");

        return ChatbotResponse.builder()
                .replyType("TEXT")
                .message(response)
                .build();
    }

    // =========================================================================
    // 9. WAITLIST AUTOMATION ASSISTANCE
    // =========================================================================
    private boolean isMyWaitlistQuery(String msg) {
        return msg.contains("my waitlist") || msg.contains("show waitlist") || msg.contains("view waitlist")
                || msg.contains("waitlist status") || msg.contains("check waitlist") || msg.contains("is there any update on my waitlist")
                || msg.contains("got a slot notification") || msg.contains("waitlist update") || msg.equals("waitlist");
    }

    private boolean isCancelWaitlistIntent(String msg) {
        return msg.contains("cancel waitlist") || msg.contains("leave waitlist")
                || msg.contains("remove from waitlist") || msg.contains("exit waitlist") || msg.contains("delete waitlist");
    }

    private boolean isConfirmWaitlistOfferIntent(String msg) {
        return msg.contains("confirm the available slot") || msg.contains("confirm offered slot")
                || msg.contains("confirm waitlist") || msg.contains("accept slot")
                || msg.contains("accept offered slot") || msg.contains("accept waitlist slot");
    }

    private boolean isJoinWaitlistIntent(String msg) {
        return msg.contains("join waitlist") || msg.contains("add to waitlist") || msg.contains("put me on waitlist")
                || msg.contains("join the waitlist");
    }

    private ChatbotResponse handleWaitlistOfferConfirmation(ChatbotRequest request, Patient patient, String msg) {
        if (msg.contains("no") || msg.contains("cancel") || msg.contains("dont") || msg.contains("don't") || msg.contains("not now")) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("No problem! Let me know if you would like to search for another doctor or choose a different date.")
                    .conversationState("NONE")
                    .build();
        }

        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ You need an active patient medical profile to join the waitlist. Please register your patient profile first.")
                    .conversationState("NONE")
                    .build();
        }

        Map<String, Object> ctx = request.getContext();
        Long doctorId = request.getSelectedDoctorId();
        String dateStr = request.getSelectedDate();
        String preferredTimeStr = request.getSelectedTime();
        String reason = request.getReason();

        if (ctx != null) {
            if (doctorId == null && ctx.get("doctorId") != null) {
                doctorId = Long.valueOf(ctx.get("doctorId").toString());
            }
            if (dateStr == null && ctx.get("date") != null) {
                dateStr = ctx.get("date").toString();
            }
            if (preferredTimeStr == null && ctx.get("time") != null) {
                preferredTimeStr = ctx.get("time").toString();
            }
            if (reason == null && ctx.get("reason") != null) {
                reason = ctx.get("reason").toString();
            }
        }

        if (doctorId == null || dateStr == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("Missing doctor or appointment date details to join waitlist. Please ask again specifying the doctor and date.")
                    .conversationState("NONE")
                    .build();
        }

        if (reason == null || reason.isBlank()) {
            reason = "Patient consultation (Waitlist registration)";
        }

        LocalDate date = LocalDate.parse(dateStr);
        LocalTime prefTime = (preferredTimeStr != null && !preferredTimeStr.isBlank()) ?
                LocalTime.parse(preferredTimeStr.length() == 5 ? preferredTimeStr + ":00" : preferredTimeStr) : null;

        try {
            com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest waitlistReq =
                    com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest.builder()
                            .doctorId(doctorId)
                            .appointmentDate(date)
                            .preferredTime(prefTime)
                            .reasonForVisit(reason)
                            .build();

            com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse saved =
                    waitlistService.joinWaitlist(waitlistReq);

            String timeInfo = saved.getPreferredTime() != null ? "at " + saved.getPreferredTime() : "Any available time";
            String successMsg = "✅ **You have been added to the waitlist!**\n\n"
                    + "• **Doctor**: Dr. " + saved.getDoctorName() + "\n"
                    + "• **Date**: " + saved.getAppointmentDate() + "\n"
                    + "• **Preferred Time**: " + timeInfo + "\n"
                    + "• **Queue Position**: #" + saved.getQueuePosition() + "\n\n"
                    + "If an existing appointment is cancelled or becomes available, you will receive an automatic in-app notification with an opportunity to confirm.";

            return ChatbotResponse.builder()
                    .replyType("WAITLIST_JOINED")
                    .message(successMsg)
                    .waitlistEntry(saved)
                    .conversationState("NONE")
                    .build();
        } catch (BadRequestException ex) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ " + ex.getMessage())
                    .conversationState("NONE")
                    .build();
        }
    }

    private ChatbotResponse handleMyWaitlistQuery(Patient patient) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("You do not have a patient medical profile registered yet. Please register your patient profile to view waitlist entries.")
                    .build();
        }

        List<com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse> entries = waitlistService.getMyWaitlist();
        if (entries.isEmpty()) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("You are not currently on any doctor waitlists. If a doctor is fully booked, you can join their waitlist anytime!")
                    .build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 **Your Active & Past Waitlist Entries**:\n\n");

        for (com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse w : entries) {
            String statusBadge = switch (w.getStatus()) {
                case WAITING -> "⏳ [WAITING - Position #" + w.getQueuePosition() + "]";
                case NOTIFIED -> "🎉 [SLOT OFFERED - Action Required!]";
                case BOOKED -> "✅ [BOOKED]";
                case CANCELLED -> "❌ [CANCELLED]";
                case EXPIRED -> "⏰ [EXPIRED]";
            };

            String timePref = w.getPreferredTime() != null ? w.getPreferredTime().toString() : "Any time";
            sb.append("• **Waitlist #").append(w.getId()).append("** ").append(statusBadge).append("\n")
                    .append("   Doctor: Dr. ").append(w.getDoctorName()).append(" (").append(w.getDoctorSpecialization()).append(")\n")
                    .append("   Target Date: ").append(w.getAppointmentDate()).append(" • Preference: ").append(timePref).append("\n");

            if (w.isOfferActive()) {
                sb.append("   ⭐ **Offered Slot**: ").append(w.getOfferedTime()).append(" (Expires: ").append(w.getExpiresAt()).append(")\n")
                        .append("   👉 Reply *'Confirm waitlist slot'* or *'Confirm offered slot'* to book now!\n");
            }
            sb.append("\n");
        }

        return ChatbotResponse.builder()
                .replyType("WAITLIST_LIST")
                .message(sb.toString())
                .waitlistEntries(entries)
                .build();
    }

    private ChatbotResponse handleCancelWaitlist(ChatbotRequest request, Patient patient, String msg) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("You must be logged in as a patient to cancel a waitlist entry.")
                    .build();
        }

        Long waitlistId = request.getWaitlistIdToCancel();
        if (waitlistId == null) {
            // Check if user included a number in their message e.g. "cancel waitlist 3" or "#3"
            Pattern pattern = Pattern.compile("(?:#|entry\\s*|waitlist\\s*)(\\d+)");
            Matcher matcher = pattern.matcher(msg);
            if (matcher.find()) {
                try {
                    waitlistId = Long.parseLong(matcher.group(1));
                } catch (Exception ignored) {
                }
            }
        }

        if (waitlistId == null) {
            List<com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse> entries = waitlistService.getMyWaitlist();
            List<com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse> activeEntries = entries.stream()
                    .filter(e -> e.getStatus() == com.example.healthcareappointmentmanagementsystem.entity.WaitlistStatus.WAITING
                            || e.getStatus() == com.example.healthcareappointmentmanagementsystem.entity.WaitlistStatus.NOTIFIED)
                    .collect(Collectors.toList());

            if (activeEntries.size() == 1) {
                waitlistId = activeEntries.get(0).getId();
            } else if (activeEntries.isEmpty()) {
                return ChatbotResponse.builder()
                        .replyType("TEXT")
                        .message("You do not have any active waitlist entries to cancel.")
                        .build();
            } else {
                return ChatbotResponse.builder()
                        .replyType("TEXT")
                        .message("You have multiple active waitlist entries. Please specify which waitlist entry ID to cancel (e.g., *'Cancel waitlist #" + activeEntries.get(0).getId() + "'*).")
                        .waitlistEntries(activeEntries)
                        .build();
            }
        }

        try {
            com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse cancelled =
                    waitlistService.cancelWaitlistEntry(waitlistId);

            return ChatbotResponse.builder()
                    .replyType("WAITLIST_CANCELLED")
                    .message("✅ Your waitlist entry **#" + cancelled.getId() + "** for Dr. **" + cancelled.getDoctorName() + "** on " + cancelled.getAppointmentDate() + " has been cancelled.")
                    .waitlistEntry(cancelled)
                    .build();
        } catch (BadRequestException ex) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ " + ex.getMessage())
                    .build();
        }
    }

    private ChatbotResponse handleConfirmWaitlistOffer(ChatbotRequest request, Patient patient, String msg) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("You must be logged in as a patient to confirm a waitlist slot offer.")
                    .build();
        }

        Long waitlistId = request.getWaitlistIdToConfirm();
        if (waitlistId == null) {
            Pattern pattern = Pattern.compile("(?:#|entry\\s*|waitlist\\s*)(\\d+)");
            Matcher matcher = pattern.matcher(msg);
            if (matcher.find()) {
                try {
                    waitlistId = Long.parseLong(matcher.group(1));
                } catch (Exception ignored) {
                }
            }
        }

        if (waitlistId == null) {
            List<com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse> entries = waitlistService.getMyWaitlist();
            List<com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse> activeOffers = entries.stream()
                    .filter(com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse::isOfferActive)
                    .collect(Collectors.toList());

            if (activeOffers.size() == 1) {
                waitlistId = activeOffers.get(0).getId();
            } else if (activeOffers.isEmpty()) {
                return ChatbotResponse.builder()
                        .replyType("TEXT")
                        .message("You do not currently have any active slot offers to confirm. We will notify you as soon as a slot opens up!")
                        .build();
            } else {
                return ChatbotResponse.builder()
                        .replyType("TEXT")
                        .message("You have multiple slot offers available. Please specify the waitlist entry ID to confirm (e.g. *'Confirm waitlist #" + activeOffers.get(0).getId() + "'*).")
                        .waitlistEntries(activeOffers)
                        .build();
            }
        }

        try {
            AppointmentResponse appt = waitlistService.confirmOfferedSlot(waitlistId);

            String successMsg = "🎉 **Slot Confirmed and Appointment Booked!**\n\n"
                    + "• **Appointment ID**: #" + appt.getId() + "\n"
                    + "• **Doctor**: Dr. " + appt.getDoctorName() + "\n"
                    + "• **Date**: " + appt.getAppointmentDate() + "\n"
                    + "• **Time**: " + appt.getAppointmentTime() + "\n"
                    + "• **Status**: " + appt.getStatus() + "\n\n"
                    + "Your waitlist offer has been converted into an official appointment. You will receive an in-app reminder 1 hour before your visit.";

            return ChatbotResponse.builder()
                    .replyType("WAITLIST_CONFIRMED")
                    .message(successMsg)
                    .appointments(List.of(appt))
                    .build();
        } catch (BadRequestException ex) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ " + ex.getMessage())
                    .build();
        }
    }

    private ChatbotResponse handleJoinWaitlistDirect(ChatbotRequest request, Patient patient, String msg) {
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ To join a waitlist, you must have an active patient profile. Please complete your patient profile setup.")
                    .build();
        }

        List<DoctorResponse> doctors = doctorService.getAllDoctors();
        Long doctorId = request.getSelectedDoctorId();

        if (doctorId == null) {
            for (DoctorResponse d : doctors) {
                if (msg.contains(d.getFullName().toLowerCase())) {
                    doctorId = d.getId();
                    break;
                }
            }
        }

        if (doctorId == null && !doctors.isEmpty()) {
            doctorId = doctors.get(0).getId();
        }

        if (doctorId == null) {
            return ChatbotResponse.builder()
                    .replyType("DOCTOR_LIST")
                    .message("Which doctor would you like to join the waitlist for?")
                    .doctors(doctors)
                    .nextAction("SELECT_DOCTOR")
                    .build();
        }

        DoctorResponse doc = doctorService.getDoctorById(doctorId);
        LocalDate targetDate = parseDateFromMessage(msg, request.getSelectedDate());
        String preferredTimeStr = request.getSelectedTime() != null ? request.getSelectedTime() : parseTimeFromMessage(msg);

        LocalTime prefTime = (preferredTimeStr != null && !preferredTimeStr.isBlank()) ?
                LocalTime.parse(preferredTimeStr.length() == 5 ? preferredTimeStr + ":00" : preferredTimeStr) : null;

        try {
            com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest waitlistReq =
                    com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest.builder()
                            .doctorId(doctorId)
                            .appointmentDate(targetDate)
                            .preferredTime(prefTime)
                            .reasonForVisit("Patient consultation (Waitlist)")
                            .build();

            com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse saved =
                    waitlistService.joinWaitlist(waitlistReq);

            String timeInfo = saved.getPreferredTime() != null ? "at " + saved.getPreferredTime() : "Any available time";
            String successMsg = "✅ **You have been added to the waitlist!**\n\n"
                    + "• **Doctor**: Dr. " + saved.getDoctorName() + "\n"
                    + "• **Date**: " + saved.getAppointmentDate() + "\n"
                    + "• **Preferred Time**: " + timeInfo + "\n"
                    + "• **Queue Position**: #" + saved.getQueuePosition() + "\n\n"
                    + "We will notify you immediately when a matching slot becomes available.";

            return ChatbotResponse.builder()
                    .replyType("WAITLIST_JOINED")
                    .message(successMsg)
                    .waitlistEntry(saved)
                    .build();
        } catch (BadRequestException ex) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ " + ex.getMessage())
                    .build();
        }
    }

    // =========================================================================
    // DATE & TIME PARSING HELPERS
    // =========================================================================
    private LocalDate parseDateFromMessage(String msg, String selectedDate) {
        if (selectedDate != null && !selectedDate.isBlank()) {
            try {
                return LocalDate.parse(selectedDate);
            } catch (Exception ignored) {
            }
        }

        if (msg.contains("tomorrow")) {
            return LocalDate.now().plusDays(1);
        }
        if (msg.contains("day after tomorrow")) {
            return LocalDate.now().plusDays(2);
        }
        if (msg.contains("next week")) {
            return LocalDate.now().plusWeeks(1);
        }
        if (msg.contains("today")) {
            return LocalDate.now();
        }

        // Match YYYY-MM-DD
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher m = datePattern.matcher(msg);
        if (m.find()) {
            try {
                return LocalDate.parse(m.group(1));
            } catch (Exception ignored) {
            }
        }

        // Default to tomorrow for upcoming slot queries
        return LocalDate.now().plusDays(1);
    }

    private String parseTimeFromMessage(String msg) {
        // Match HH:mm or HH:mm AM/PM
        Pattern timePattern = Pattern.compile("(\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b)");
        Matcher m = timePattern.matcher(msg);
        if (m.find()) {
            return m.group(1);
        }

        // Match "10 am", "2 pm", "10:30 am"
        Pattern ampmPattern = Pattern.compile("(\\b(?:1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(?:am|pm)\\b)", Pattern.CASE_INSENSITIVE);
        Matcher ampmMatcher = ampmPattern.matcher(msg);
        if (ampmMatcher.find()) {
            String match = ampmMatcher.group(1).trim().toUpperCase();
            try {
                if (match.contains(":")) {
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
                    return LocalTime.parse(match, f).format(DateTimeFormatter.ofPattern("HH:mm"));
                } else {
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH);
                    return LocalTime.parse(match, f).format(DateTimeFormatter.ofPattern("HH:mm"));
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    // =========================================================================
    // 10. NO-SHOW QUERY & ASSISTANCE
    // =========================================================================
    private boolean isNoShowQuery(String msg) {
        return msg.contains("no show") || msg.contains("no-show") || msg.contains("noshow")
                || msg.contains("missed appointment") || msg.contains("missed visit")
                || msg.contains("did not attend") || msg.contains("didn't attend");
    }

    private ChatbotResponse handleNoShowInquiry(ChatbotRequest request, Patient patient, User user, String msg) {
        // Check if user is asking to mark an appointment as no-show
        if (msg.contains("mark") || msg.contains("set") || msg.contains("flag")) {
            boolean isDoctor = user.getRole() == com.example.healthcareappointmentmanagementsystem.entity.Role.DOCTOR;
            boolean isAdmin = user.getRole() == com.example.healthcareappointmentmanagementsystem.entity.Role.ADMIN;

            if (!isDoctor && !isAdmin) {
                return ChatbotResponse.builder()
                        .replyType("ERROR")
                        .message("⚠️ Patients are not authorized to mark appointments as no-show. Only consulting physicians and hospital administrators can perform this action.")
                        .build();
            }

            // Extract appointment ID
            Long apptId = extractAppointmentId(msg);
            if (apptId == null) {
                return ChatbotResponse.builder()
                        .replyType("TEXT")
                        .message("Please specify the appointment ID you want to mark as no-show (e.g. *'Mark appointment 101 as no show'*).")
                        .build();
            }

            try {
                AppointmentResponse updated = appointmentService.markAppointmentAsNoShow(apptId, "Marked via CarePortal AI Assistant");
                return ChatbotResponse.builder()
                        .replyType("NO_SHOW_MARKED")
                        .message(String.format("✅ Appointment #%d with %s has been successfully marked as **NO_SHOW**.\n\n"
                                        + "• Patient: %s\n"
                                        + "• Date: %s at %s\n"
                                        + "• A notification has been dispatched to the patient and any waiting list candidates have been processed for the slot.",
                                updated.getId(), updated.getDoctorName(), updated.getPatientName(),
                                updated.getAppointmentDate(), updated.getAppointmentTime()))
                        .appointmentId(updated.getId())
                        .build();
            } catch (Exception e) {
                return ChatbotResponse.builder()
                        .replyType("ERROR")
                        .message("⚠️ Unable to mark appointment #" + apptId + " as no-show: " + e.getMessage())
                        .build();
            }
        }

        // Otherwise: inquiry about missed/no-show appointments or explanation
        if (patient == null) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("⚠️ Please link or register your Patient Profile to check your appointment attendance history.")
                    .build();
        }

        List<Appointment> allAppts = appointmentRepository.findByPatient(patient);
        List<Appointment> noShowAppts = allAppts.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.NO_SHOW)
                .collect(Collectors.toList());

        if (msg.contains("why") || msg.contains("reason") || msg.contains("what happened")) {
            if (noShowAppts.isEmpty()) {
                return ChatbotResponse.builder()
                        .replyType("TEXT")
                        .message("You currently have **no appointments marked as NO_SHOW**.")
                        .build();
            }
            Appointment latest = noShowAppts.get(noShowAppts.size() - 1);
            String docName = latest.getDoctor() != null && latest.getDoctor().getUser() != null
                    ? "Dr. " + latest.getDoctor().getUser().getFirstName() + " " + latest.getDoctor().getUser().getLastName()
                    : "your doctor";
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message(String.format("ℹ️ Your appointment #%d on **%s at %s** with **%s** is currently marked as **NO_SHOW** because attendance was not recorded for that scheduled consultation.\n\n"
                                    + "Reason noted: *\"%s\"*.\n\n"
                                    + "If you believe this was an error, please reach out to the hospital administration.",
                            latest.getId(), latest.getAppointmentDate(), latest.getAppointmentTime(), docName,
                            latest.getNoShowReason() != null ? latest.getNoShowReason() : "Non-attendance"))
                    .build();
        }

        if (noShowAppts.isEmpty()) {
            return ChatbotResponse.builder()
                    .replyType("TEXT")
                    .message("👍 You have **0 missed visits**. None of your appointments are marked as NO_SHOW.")
                    .build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 **Your Missed Appointments (NO_SHOW)**:\n\n");
        for (Appointment a : noShowAppts) {
            String docName = a.getDoctor() != null && a.getDoctor().getUser() != null
                    ? "Dr. " + a.getDoctor().getUser().getFirstName() + " " + a.getDoctor().getUser().getLastName()
                    : "Doctor";
            sb.append(String.format("• **Appt #%d** with **%s** on %s at %s (Status: `NO_SHOW`)\n",
                    a.getId(), docName, a.getAppointmentDate(), a.getAppointmentTime()));
        }
        sb.append("\nYou can book a new appointment at any time using *'Book an appointment'*.");

        List<AppointmentResponse> respList = noShowAppts.stream().map(appointmentMapper::toResponse).collect(Collectors.toList());

        return ChatbotResponse.builder()
                .replyType("APPOINTMENT_LIST")
                .message(sb.toString())
                .appointments(respList)
                .build();
    }

    private Long extractAppointmentId(String msg) {
        Pattern p = Pattern.compile("(?:#|appointment\\s+|appt\\s+|id\\s+)?(\\d+)");
        Matcher m = p.matcher(msg);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
