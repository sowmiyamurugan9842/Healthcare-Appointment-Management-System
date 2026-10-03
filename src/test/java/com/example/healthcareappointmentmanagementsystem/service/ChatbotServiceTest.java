package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.request.ChatbotRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.ChatbotResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorAvailableSlotsResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.DoctorResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;
import com.example.healthcareappointmentmanagementsystem.entity.Appointment;
import com.example.healthcareappointmentmanagementsystem.entity.AppointmentStatus;
import com.example.healthcareappointmentmanagementsystem.entity.Department;
import com.example.healthcareappointmentmanagementsystem.entity.Doctor;
import com.example.healthcareappointmentmanagementsystem.entity.Patient;
import com.example.healthcareappointmentmanagementsystem.entity.Role;
import com.example.healthcareappointmentmanagementsystem.entity.User;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.mapper.DoctorMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DepartmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.UserRepository;
import com.example.healthcareappointmentmanagementsystem.service.impl.ChatbotServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatbotServiceTest {

    @Mock
    private DoctorService doctorService;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PrescriptionService prescriptionService;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private AIService aiService;

    @InjectMocks
    private ChatbotServiceImpl chatbotService;

    private User testUser;
    private Patient testPatient;
    private Doctor testDoctor;
    private Department testDept;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("Sowmiya")
                .lastName("Murugan")
                .email("sowmiya@careportal.com")
                .role(Role.PATIENT)
                .build();

        testPatient = Patient.builder()
                .id(10L)
                .user(testUser)
                .build();

        testDept = Department.builder()
                .id(1L)
                .departmentName("Cardiology")
                .description("Heart and cardiovascular care")
                .build();

        User docUser = User.builder()
                .id(2L)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@careportal.com")
                .role(Role.DOCTOR)
                .build();

        testDoctor = Doctor.builder()
                .id(5L)
                .user(docUser)
                .department(testDept)
                .specialization("Cardiology")
                .qualification("MD, FACC")
                .consultationFee(120.00)
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .build();

        // Setup Spring Security Context
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                testUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Safety Rule: Should trigger safety warning on medical diagnosis/prescription inquiries")
    void testProcessMessage_SafetyWarningOnMedicalDiagnosis() {
        ChatbotRequest request = ChatbotRequest.builder()
                .message("Can you diagnose my chest pain and tell me what medicine to take?")
                .build();

        ChatbotResponse response = chatbotService.processMessage(request);

        assertNotNull(response);
        assertEquals("SAFETY_WARNING", response.getReplyType());
        assertTrue(response.getMessage().contains("Medical Notice"));
        assertTrue(response.getMessage().contains("cannot provide medical diagnoses"));
    }

    @Test
    @DisplayName("Doctor Search: Should return list of cardiologists when asked for cardiology doctors")
    void testProcessMessage_DoctorSearchSpecialist() {
        when(userRepository.findByEmail("sowmiya@careportal.com")).thenReturn(Optional.of(testUser));
        when(patientRepository.findByUser(testUser)).thenReturn(Optional.of(testPatient));

        DoctorResponse docRes = DoctorResponse.builder()
                .id(5L)
                .fullName("Alice Smith")
                .specialization("Cardiology")
                .departmentName("Cardiology")
                .qualification("MD")
                .consultationFee(120.00)
                .experienceYears(10)
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .build();

        when(doctorService.getAllDoctors()).thenReturn(List.of(docRes));

        ChatbotRequest request = ChatbotRequest.builder()
                .message("I need a cardiologist")
                .build();

        ChatbotResponse response = chatbotService.processMessage(request);

        assertNotNull(response);
        assertEquals("DOCTOR_LIST", response.getReplyType());
        assertNotNull(response.getDoctors());
        assertEquals(1, response.getDoctors().size());
        assertEquals("Alice Smith", response.getDoctors().get(0).getFullName());
        assertTrue(response.getMessage().contains("Dr. Alice Smith"));
    }

    @Test
    @DisplayName("Available Slots: Should return open 30-min slots for requested doctor and date")
    void testProcessMessage_AvailableSlotsQuery() {
        when(userRepository.findByEmail("sowmiya@careportal.com")).thenReturn(Optional.of(testUser));
        when(patientRepository.findByUser(testUser)).thenReturn(Optional.of(testPatient));

        DoctorResponse docRes = DoctorResponse.builder()
                .id(5L)
                .fullName("Alice Smith")
                .specialization("Cardiology")
                .departmentName("Cardiology")
                .build();

        when(doctorService.getAllDoctors()).thenReturn(List.of(docRes));
        when(doctorService.getDoctorById(5L)).thenReturn(docRes);

        LocalDate targetDate = LocalDate.now().plusDays(1);
        DoctorAvailableSlotsResponse slotsRes = DoctorAvailableSlotsResponse.builder()
                .doctorId(5L)
                .date(targetDate)
                .availableSlots(List.of("09:00", "09:30", "10:30", "11:00"))
                .build();

        when(doctorService.getAvailableSlots(eq(5L), any(LocalDate.class))).thenReturn(slotsRes);

        ChatbotRequest request = ChatbotRequest.builder()
                .message("What slots are available for Dr. Alice Smith tomorrow?")
                .build();

        ChatbotResponse response = chatbotService.processMessage(request);

        assertNotNull(response);
        assertEquals("SLOTS_LIST", response.getReplyType());
        assertEquals(5L, response.getDoctorId());
        assertEquals(4, response.getAvailableSlots().size());
        assertTrue(response.getAvailableSlots().contains("09:00"));
        assertTrue(response.getMessage().contains("Available 30-minute consultation slots"));
    }

    @Test
    @DisplayName("My Appointments: Should return scheduled appointments for logged-in patient")
    void testProcessMessage_MyAppointments() {
        when(userRepository.findByEmail("sowmiya@careportal.com")).thenReturn(Optional.of(testUser));
        when(patientRepository.findByUser(testUser)).thenReturn(Optional.of(testPatient));

        Appointment appt = Appointment.builder()
                .id(101L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .reasonForVisit("Follow-up checkup")
                .build();

        AppointmentResponse apptRes = AppointmentResponse.builder()
                .id(101L)
                .doctorName("Alice Smith")
                .patientName("Sowmiya Murugan")
                .departmentName("Cardiology")
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .reasonForVisit("Follow-up checkup")
                .build();

        when(appointmentRepository.findByPatient(testPatient)).thenReturn(List.of(appt));
        when(appointmentMapper.toResponse(appt)).thenReturn(apptRes);

        ChatbotRequest request = ChatbotRequest.builder()
                .message("Show my upcoming appointments")
                .build();

        ChatbotResponse response = chatbotService.processMessage(request);

        assertNotNull(response);
        assertEquals("APPOINTMENT_LIST", response.getReplyType());
        assertEquals(1, response.getAppointments().size());
        assertEquals(101L, response.getAppointments().get(0).getId());
        assertTrue(response.getMessage().contains("Appointment #101"));
    }

    @Test
    @DisplayName("Guided Booking Flow: Generates confirmation prompt card and books upon confirmation")
    void testProcessMessage_GuidedBookingFlow_ConfirmationAndSuccess() {
        when(userRepository.findByEmail("sowmiya@careportal.com")).thenReturn(Optional.of(testUser));
        when(patientRepository.findByUser(testUser)).thenReturn(Optional.of(testPatient));

        DoctorResponse docRes = DoctorResponse.builder()
                .id(5L)
                .fullName("Alice Smith")
                .specialization("Cardiology")
                .departmentName("Cardiology")
                .consultationFee(120.00)
                .build();

        when(doctorService.getDoctorById(5L)).thenReturn(docRes);

        // Turn 1: User selects slot 10:00 -> Generates CONFIRMATION_PROMPT
        ChatbotRequest turn1Req = ChatbotRequest.builder()
                .message("I want to book 10:00 AM on 2026-10-10")
                .selectedDoctorId(5L)
                .selectedDate("2026-10-10")
                .selectedTime("10:00")
                .reason("Cardiac consultation")
                .build();

        ChatbotResponse turn1Res = chatbotService.processMessage(turn1Req);

        assertNotNull(turn1Res);
        assertEquals("CONFIRMATION_PROMPT", turn1Res.getReplyType());
        assertEquals("AWAITING_CONFIRMATION", turn1Res.getConversationState());
        assertTrue(turn1Res.getMessage().contains("Please review your appointment booking details"));

        // Turn 2: User confirms -> Books appointment via AppointmentService
        AppointmentResponse bookedAppt = AppointmentResponse.builder()
                .id(202L)
                .doctorName("Alice Smith")
                .patientName("Sowmiya Murugan")
                .appointmentDate(LocalDate.parse("2026-10-10"))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentService.bookAppointment(any(AppointmentRequest.class))).thenReturn(bookedAppt);

        ChatbotRequest turn2Req = ChatbotRequest.builder()
                .message("Yes, confirm booking")
                .conversationState("AWAITING_CONFIRMATION")
                .selectedDoctorId(5L)
                .selectedDate("2026-10-10")
                .selectedTime("10:00")
                .reason("Cardiac consultation")
                .context(Map.of(
                        "doctorId", 5L,
                        "date", "2026-10-10",
                        "time", "10:00:00",
                        "reason", "Cardiac consultation"
                ))
                .build();

        ChatbotResponse turn2Res = chatbotService.processMessage(turn2Req);

        assertNotNull(turn2Res);
        assertEquals("BOOKING_SUCCESS", turn2Res.getReplyType());
        assertTrue(turn2Res.getMessage().contains("Appointment Confirmed Successfully!"));
        assertTrue(turn2Res.getMessage().contains("#202"));
    }

    @Test
    @DisplayName("Cancellation: Cancels appointment successfully when requested")
    void testProcessMessage_CancelAppointment() {
        when(userRepository.findByEmail("sowmiya@careportal.com")).thenReturn(Optional.of(testUser));
        when(patientRepository.findByUser(testUser)).thenReturn(Optional.of(testPatient));

        AppointmentResponse cancelledRes = AppointmentResponse.builder()
                .id(101L)
                .doctorName("Alice Smith")
                .appointmentDate(LocalDate.now().plusDays(2))
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentService.cancelAppointment(101L)).thenReturn(cancelledRes);

        ChatbotRequest request = ChatbotRequest.builder()
                .message("Cancel appointment #101")
                .appointmentIdToCancel(101L)
                .build();

        ChatbotResponse response = chatbotService.processMessage(request);

        assertNotNull(response);
        assertEquals("CANCELLATION_SUCCESS", response.getReplyType());
        assertTrue(response.getMessage().contains("Appointment #101 has been successfully cancelled"));
    }

    @Test
    @DisplayName("Departments: Returns list of hospital departments")
    void testProcessMessage_DepartmentQuery() {
        when(userRepository.findByEmail("sowmiya@careportal.com")).thenReturn(Optional.of(testUser));
        when(patientRepository.findByUser(testUser)).thenReturn(Optional.of(testPatient));

        when(departmentRepository.findAll()).thenReturn(List.of(testDept));

        ChatbotRequest request = ChatbotRequest.builder()
                .message("What departments are available?")
                .build();

        ChatbotResponse response = chatbotService.processMessage(request);

        assertNotNull(response);
        assertNotNull(response.getDepartments());
        assertEquals(1, response.getDepartments().size());
        assertEquals("Cardiology", response.getDepartments().get(0).getDepartmentName());
    }

    @Test
    @DisplayName("Prescriptions: Returns prescriptions without medical generation")
    void testProcessMessage_PrescriptionQuery() {
        when(userRepository.findByEmail("sowmiya@careportal.com")).thenReturn(Optional.of(testUser));
        when(patientRepository.findByUser(testUser)).thenReturn(Optional.of(testPatient));

        PrescriptionResponse rxRes = PrescriptionResponse.builder()
                .id(77L)
                .doctorName("Alice Smith")
                .diagnosis("Mild Hypertension")
                .medications("Amlodipine 5mg once daily")
                .doctorAdvice("Reduce sodium intake and stay hydrated")
                .build();

        when(prescriptionService.getPrescriptionsByPatient(10L)).thenReturn(List.of(rxRes));

        ChatbotRequest request = ChatbotRequest.builder()
                .message("Show my prescriptions")
                .build();

        ChatbotResponse response = chatbotService.processMessage(request);

        assertNotNull(response);
        assertEquals("PRESCRIPTION_LIST", response.getReplyType());
        assertEquals(1, response.getPrescriptions().size());
        assertTrue(response.getMessage().contains("Mild Hypertension"));
        assertTrue(response.getMessage().contains("Amlodipine 5mg"));
    }

    @Test
    @DisplayName("Validation: Throws BadRequestException for empty or null message")
    void testProcessMessage_EmptyMessageThrowsBadRequestException() {
        ChatbotRequest request = ChatbotRequest.builder()
                .message("   ")
                .build();

        assertThrows(BadRequestException.class, () -> chatbotService.processMessage(request));
    }

    @Test
    @DisplayName("Security: Throws UnauthorizedException when user context is missing")
    void testProcessMessage_UnauthenticatedThrowsUnauthorizedException() {
        SecurityContextHolder.clearContext();

        ChatbotRequest request = ChatbotRequest.builder()
                .message("Hello")
                .build();

        assertThrows(UnauthorizedException.class, () -> chatbotService.processMessage(request));
    }
}
