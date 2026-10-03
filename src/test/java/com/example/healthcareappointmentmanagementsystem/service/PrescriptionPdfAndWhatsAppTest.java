package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionMedicineDto;
import com.example.healthcareappointmentmanagementsystem.dto.request.PrescriptionRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.PrescriptionResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.WhatsAppDeliveryResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.PrescriptionMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PrescriptionRepository;
import com.example.healthcareappointmentmanagementsystem.service.impl.PrescriptionPdfServiceImpl;
import com.example.healthcareappointmentmanagementsystem.service.impl.PrescriptionServiceImpl;
import com.example.healthcareappointmentmanagementsystem.service.impl.WhatsAppServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionPdfAndWhatsAppTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PrescriptionMapper prescriptionMapper;

    @Mock
    private WhatsAppService whatsAppService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RestTemplate restTemplate;

    private PrescriptionPdfService prescriptionPdfService;
    private PrescriptionServiceImpl prescriptionService;

    private Doctor testDoctor;
    private Patient testPatient;
    private Appointment testAppointment;
    private Prescription testPrescription;
    private User doctorUser;
    private User patientUser;
    private User otherPatientUser;

    @BeforeEach
    void setUp() {
        prescriptionPdfService = new PrescriptionPdfServiceImpl();

        prescriptionService = new PrescriptionServiceImpl(
                prescriptionRepository,
                appointmentRepository,
                patientRepository,
                prescriptionMapper,
                prescriptionPdfService,
                whatsAppService,
                notificationService
        );

        doctorUser = User.builder()
                .id(1L)
                .firstName("Sarah")
                .lastName("Connor")
                .email("doctor@careportal.com")
                .role(Role.DOCTOR)
                .phoneNumber("9876543210")
                .build();

        Department department = Department.builder()
                .id(1L)
                .departmentName("Cardiology")
                .description("Heart Care")
                .build();

        testDoctor = Doctor.builder()
                .id(10L)
                .user(doctorUser)
                .department(department)
                .qualification("MBBS, MD")
                .specialization("Cardiologist")
                .experienceYears(12)
                .consultationFee(150.0)
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .build();

        patientUser = User.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .email("patient@careportal.com")
                .role(Role.PATIENT)
                .phoneNumber("9876543211")
                .build();

        otherPatientUser = User.builder()
                .id(3L)
                .firstName("Jane")
                .lastName("Smith")
                .email("otherpatient@careportal.com")
                .role(Role.PATIENT)
                .phoneNumber("9876543212")
                .build();

        testPatient = Patient.builder()
                .id(20L)
                .user(patientUser)
                .dateOfBirth(LocalDate.of(1988, 5, 20))
                .gender(Gender.MALE)
                .bloodGroup(BloodGroup.O_POSITIVE)
                .address("456 Medical Boulevard")
                .emergencyContact("9876543211")
                .build();

        testAppointment = Appointment.builder()
                .id(100L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.now())
                .appointmentTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.COMPLETED)
                .reasonForVisit("Hypertension check")
                .build();

        PrescriptionMedicine med1 = PrescriptionMedicine.builder()
                .medicineName("Amlodipine")
                .dosage("5mg")
                .frequency("Once daily")
                .duration("30 days")
                .instructions("Take in the morning with water")
                .build();

        testPrescription = Prescription.builder()
                .id(50L)
                .appointment(testAppointment)
                .diagnosis("Stage 1 Primary Hypertension")
                .doctorAdvice("Reduce sodium intake and engage in 30 mins aerobic walking.")
                .additionalNotes("Monitor blood pressure daily.")
                .nextVisitDate(LocalDate.now().plusMonths(1))
                .medicines(List.of(med1))
                .whatsappStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String email, Role role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("1. Prescription PDF Generation generates valid binary PDF with header and metadata")
    void testPrescriptionPdfGeneration_Success() {
        byte[] pdfBytes = prescriptionPdfService.generatePrescriptionPdf(testPrescription);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF byte array should have meaningful size");
        // Check for PDF magic bytes "%PDF-"
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("2. Patient can download their own prescription PDF")
    void testGetPrescriptionPdf_PatientOwnAccess_Success() {
        authenticate("patient@careportal.com", Role.PATIENT);
        when(prescriptionRepository.findById(50L)).thenReturn(Optional.of(testPrescription));

        byte[] pdf = prescriptionService.getPrescriptionPdf(50L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("3. Doctor can download prescription for their assigned appointment")
    void testGetPrescriptionPdf_DoctorOwnAppointment_Success() {
        authenticate("doctor@careportal.com", Role.DOCTOR);
        when(prescriptionRepository.findById(50L)).thenReturn(Optional.of(testPrescription));

        byte[] pdf = prescriptionService.getPrescriptionPdf(50L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("4. Admin can download any prescription PDF")
    void testGetPrescriptionPdf_AdminAccess_Success() {
        authenticate("admin@careportal.com", Role.ADMIN);
        when(prescriptionRepository.findById(50L)).thenReturn(Optional.of(testPrescription));

        byte[] pdf = prescriptionService.getPrescriptionPdf(50L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("5. Patient cannot access another patient's prescription PDF (Unauthorized)")
    void testGetPrescriptionPdf_UnauthorizedPatient_ThrowsException() {
        authenticate("otherpatient@careportal.com", Role.PATIENT);
        when(prescriptionRepository.findById(50L)).thenReturn(Optional.of(testPrescription));

        assertThrows(UnauthorizedException.class, () -> prescriptionService.getPrescriptionPdf(50L));
    }

    @Test
    @DisplayName("6. Doctor cannot access another doctor's prescription PDF (Unauthorized)")
    void testGetPrescriptionPdf_UnauthorizedDoctor_ThrowsException() {
        authenticate("otherdoctor@careportal.com", Role.DOCTOR);
        when(prescriptionRepository.findById(50L)).thenReturn(Optional.of(testPrescription));

        assertThrows(UnauthorizedException.class, () -> prescriptionService.getPrescriptionPdf(50L));
    }

    @Test
    @DisplayName("7. WhatsApp service live dispatch success (Status = SENT)")
    void testWhatsAppService_DispatchSuccess() {
        WhatsAppServiceImpl waService = new WhatsAppServiceImpl(
                "https://graph.facebook.com/v19.0",
                "test_valid_access_token",
                "1092837465",
                restTemplate
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgL\"}]}", HttpStatus.OK));

        WhatsAppDeliveryResponse response = waService.sendPrescriptionPdf(testPrescription, new byte[]{1, 2, 3});

        assertNotNull(response);
        assertEquals("SENT", response.getDeliveryStatus());
        assertTrue(response.getMessage().contains("successfully delivered"));
    }

    @Test
    @DisplayName("8. WhatsApp service failure returns FAILED without throwing runtime error")
    void testWhatsAppService_DispatchFailure_HandledGracefully() {
        WhatsAppServiceImpl waService = new WhatsAppServiceImpl(
                "https://graph.facebook.com/v19.0",
                "test_valid_access_token",
                "1092837465",
                restTemplate
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection timeout to WhatsApp API"));

        WhatsAppDeliveryResponse response = waService.sendPrescriptionPdf(testPrescription, new byte[]{1, 2, 3});

        assertNotNull(response);
        assertEquals("FAILED", response.getDeliveryStatus());
        assertTrue(response.getMessage().contains("Connection timeout"));
    }

    @Test
    @DisplayName("9. WhatsApp service with missing configuration returns NOT_CONFIGURED safely")
    void testWhatsAppService_MissingConfig_ReturnsNotConfigured() {
        WhatsAppServiceImpl waService = new WhatsAppServiceImpl(
                "https://graph.facebook.com/v19.0",
                "", // empty token
                "", // empty phone id
                restTemplate
        );

        WhatsAppDeliveryResponse response = waService.sendPrescriptionPdf(testPrescription, new byte[]{1, 2, 3});

        assertNotNull(response);
        assertEquals("NOT_CONFIGURED", response.getDeliveryStatus());
        assertFalse(waService.isConfigured());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("10. Prescription creation succeeds and persists even when WhatsApp delivery fails (No Rollback)")
    void testCreatePrescription_WhatsAppFails_PrescriptionStillSaved() {
        authenticate("doctor@careportal.com", Role.DOCTOR);

        PrescriptionRequest req = PrescriptionRequest.builder()
                .appointmentId(100L)
                .diagnosis("Stage 1 Primary Hypertension")
                .doctorAdvice("Rest and diet")
                .medicines(List.of(PrescriptionMedicineDto.builder().medicineName("Amlodipine").dosage("5mg").frequency("OD").duration("30d").build()))
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(testAppointment));
        when(prescriptionRepository.findByAppointment(testAppointment)).thenReturn(Optional.empty());
        when(prescriptionMapper.toEntity(any(), any())).thenReturn(testPrescription);
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(i -> i.getArgument(0));

        // Mock WhatsApp failure
        when(whatsAppService.sendPrescriptionPdf(any(), any()))
                .thenReturn(WhatsAppDeliveryResponse.builder().deliveryStatus("FAILED").message("Meta API down").build());

        PrescriptionResponse mappedResponse = PrescriptionResponse.builder()
                .id(50L)
                .diagnosis("Stage 1 Primary Hypertension")
                .whatsappStatus("FAILED")
                .build();
        when(prescriptionMapper.toResponse(any())).thenReturn(mappedResponse);

        PrescriptionResponse result = prescriptionService.createPrescription(req);

        assertNotNull(result);
        assertEquals("FAILED", result.getWhatsappStatus());
        verify(prescriptionRepository, atLeastOnce()).save(any());
        verify(notificationService, times(1)).createPrescriptionNotification(any(), eq("FAILED"));
    }

    @Test
    @DisplayName("11. Doctor can manually resend / retry WhatsApp delivery")
    void testResendWhatsApp_Doctor_Success() {
        authenticate("doctor@careportal.com", Role.DOCTOR);

        when(prescriptionRepository.findById(50L)).thenReturn(Optional.of(testPrescription));
        when(whatsAppService.sendPrescriptionPdf(any(), any()))
                .thenReturn(WhatsAppDeliveryResponse.builder()
                        .prescriptionId(50L)
                        .deliveryStatus("SENT")
                        .message("Dispatched successfully")
                        .timestamp(LocalDateTime.now())
                        .build());

        when(prescriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WhatsAppDeliveryResponse res = prescriptionService.resendWhatsApp(50L);

        assertNotNull(res);
        assertEquals("SENT", res.getDeliveryStatus());
        assertEquals(50L, res.getPrescriptionId());
        assertEquals("SENT", testPrescription.getWhatsappStatus());
        assertNotNull(testPrescription.getWhatsappSentAt());
    }

    @Test
    @DisplayName("12. Patient cannot trigger manual WhatsApp delivery (Unauthorized)")
    void testResendWhatsApp_PatientUnauthorized_ThrowsException() {
        authenticate("patient@careportal.com", Role.PATIENT);
        when(prescriptionRepository.findById(50L)).thenReturn(Optional.of(testPrescription));

        assertThrows(UnauthorizedException.class, () -> prescriptionService.resendWhatsApp(50L));
    }
}
