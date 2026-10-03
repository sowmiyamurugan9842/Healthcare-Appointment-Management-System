package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.FollowUpRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.FollowUpResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.NotificationRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PrescriptionRepository;
import com.example.healthcareappointmentmanagementsystem.scheduler.FollowUpReminderScheduler;
import com.example.healthcareappointmentmanagementsystem.service.impl.AppointmentServiceImpl;
import com.example.healthcareappointmentmanagementsystem.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpReminderTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    private AppointmentServiceImpl appointmentService;

    private Doctor doctor1;
    private Doctor doctor2;
    private Patient patient1;
    private Patient patient2;
    private User doctorUser1;
    private User doctorUser2;
    private User patientUser1;
    private User patientUser2;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                doctorRepository,
                patientRepository,
                appointmentMapper,
                notificationService,
                notificationRepository,
                null,
                prescriptionRepository
        );

        doctorUser1 = User.builder()
                .id(1L)
                .firstName("Priya")
                .lastName("Nair")
                .email("dr.priya@careportal.com")
                .role(Role.DOCTOR)
                .build();
        doctor1 = Doctor.builder().id(101L).user(doctorUser1).specialization("Cardiology").build();

        doctorUser2 = User.builder()
                .id(2L)
                .firstName("Rajesh")
                .lastName("Kumar")
                .email("dr.rajesh@careportal.com")
                .role(Role.DOCTOR)
                .build();
        doctor2 = Doctor.builder().id(102L).user(doctorUser2).specialization("Dermatology").build();

        patientUser1 = User.builder()
                .id(10L)
                .firstName("Amit")
                .lastName("Patel")
                .email("amit@careportal.com")
                .role(Role.PATIENT)
                .phoneNumber("9876543210")
                .build();
        patient1 = Patient.builder().id(201L).user(patientUser1).build();

        patientUser2 = User.builder()
                .id(11L)
                .firstName("Sneha")
                .lastName("Rao")
                .email("sneha@careportal.com")
                .role(Role.PATIENT)
                .phoneNumber("9876543211")
                .build();
        patient2 = Patient.builder().id(202L).user(patientUser2).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsDoctor(User doctorUser) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        doctorUser.getEmail(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                )
        );
    }

    private void authenticateAsPatient(User patientUser) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        patientUser.getEmail(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))
                )
        );
    }

    @Test
    @DisplayName("1. Doctor can set follow-up date and time on their appointment")
    void test1_DoctorCanSetFollowUpDateAndTime() {
        authenticateAsDoctor(doctorUser1);

        Appointment appt = Appointment.builder()
                .id(501L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.now())
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(appointmentRepository.findById(501L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate followUpDate = LocalDate.now().plusDays(7);
        LocalTime followUpTime = LocalTime.of(10, 30);
        String followUpNotes = "Check ECG and adjust statin dose.";

        FollowUpRequest request = FollowUpRequest.builder()
                .followUpDate(followUpDate)
                .followUpTime(followUpTime)
                .followUpNotes(followUpNotes)
                .build();

        FollowUpResponse response = appointmentService.setAppointmentFollowUp(501L, request, doctorUser1.getEmail());

        assertNotNull(response);
        assertEquals(501L, response.getAppointmentId());
        assertEquals(followUpDate, response.getFollowUpDate());
        assertEquals(followUpTime, response.getFollowUpTime());
        assertEquals(followUpNotes, response.getFollowUpNotes());
        assertFalse(response.isFollowUpReminderSent());
        assertEquals(201L, response.getPatientId());
        assertEquals(101L, response.getDoctorId());

        verify(appointmentRepository, times(1)).save(appt);
        assertEquals(followUpDate, appt.getFollowUpDate());
        assertEquals(followUpTime, appt.getFollowUpTime());
        assertEquals(followUpNotes, appt.getFollowUpNotes());
        assertFalse(appt.isFollowUpReminderSent());
    }

    @Test
    @DisplayName("2. Doctor cannot modify another doctor's appointment follow-up")
    void test2_DoctorCannotModifyAnotherDoctorAppointment() {
        authenticateAsDoctor(doctorUser2); // Dr. Rajesh Kumar

        Appointment appt = Appointment.builder()
                .id(502L)
                .doctor(doctor1) // Belongs to Dr. Priya Nair
                .patient(patient1)
                .appointmentDate(LocalDate.now())
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(appointmentRepository.findById(502L)).thenReturn(Optional.of(appt));

        FollowUpRequest request = FollowUpRequest.builder()
                .followUpDate(LocalDate.now().plusDays(10))
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                appointmentService.setAppointmentFollowUp(502L, request, doctorUser2.getEmail()));

        assertTrue(ex.getMessage().contains("authorized"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Patient can view their own follow-up information")
    void test3_PatientCanViewTheirOwnFollowUp() {
        authenticateAsPatient(patientUser1);

        LocalDate followUpDate = LocalDate.now().plusDays(14);
        LocalTime followUpTime = LocalTime.of(11, 0);

        Appointment appt = Appointment.builder()
                .id(503L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.now().minusDays(1))
                .status(AppointmentStatus.COMPLETED)
                .followUpDate(followUpDate)
                .followUpTime(followUpTime)
                .followUpNotes("Routine blood pressure check")
                .followUpReminderSent(false)
                .build();

        when(appointmentRepository.findById(503L)).thenReturn(Optional.of(appt));

        FollowUpResponse response = appointmentService.getAppointmentFollowUp(503L, patientUser1.getEmail());

        assertNotNull(response);
        assertEquals(503L, response.getAppointmentId());
        assertEquals(followUpDate, response.getFollowUpDate());
        assertEquals(followUpTime, response.getFollowUpTime());
        assertEquals("Routine blood pressure check", response.getFollowUpNotes());
        assertEquals("Amit Patel", response.getPatientName());
        assertEquals("Dr. Priya Nair", response.getDoctorName());
    }

    @Test
    @DisplayName("4. Patient cannot modify follow-up information")
    void test4_PatientCannotModifyFollowUp() {
        authenticateAsPatient(patientUser1);

        Appointment appt = Appointment.builder()
                .id(504L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.now())
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(appointmentRepository.findById(504L)).thenReturn(Optional.of(appt));

        FollowUpRequest request = FollowUpRequest.builder()
                .followUpDate(LocalDate.now().plusDays(5))
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                appointmentService.setAppointmentFollowUp(504L, request, patientUser1.getEmail()));

        assertTrue(ex.getMessage().contains("authorized"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Invalid follow-up date (in the past) is rejected")
    void test5_InvalidFollowUpDate_PastDate_Rejected() {
        authenticateAsDoctor(doctorUser1);

        Appointment appt = Appointment.builder()
                .id(505L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.now())
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(appointmentRepository.findById(505L)).thenReturn(Optional.of(appt));

        FollowUpRequest request = FollowUpRequest.builder()
                .followUpDate(LocalDate.now().minusDays(2)) // Past date
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                appointmentService.setAppointmentFollowUp(505L, request, doctorUser1.getEmail()));

        assertTrue(ex.getMessage().contains("past"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Follow-up date before consultation date is rejected")
    void test6_FollowUpDateBeforeConsultationDate_Rejected() {
        authenticateAsDoctor(doctorUser1);

        LocalDate consultationDate = LocalDate.now().plusDays(5); // Future consultation

        Appointment appt = Appointment.builder()
                .id(506L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(consultationDate)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findById(506L)).thenReturn(Optional.of(appt));

        FollowUpRequest request = FollowUpRequest.builder()
                .followUpDate(consultationDate.minusDays(1)) // Before consultation
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                appointmentService.setAppointmentFollowUp(506L, request, doctorUser1.getEmail()));

        assertTrue(ex.getMessage().contains("cannot be before consultation date"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Scheduler finds upcoming follow-ups matching advance reminder window")
    void test7_SchedulerFindsUpcomingFollowUps() {
        LocalDate today = LocalDate.of(2026, 10, 9);
        LocalDate targetFollowUpDate = LocalDate.of(2026, 10, 10); // 1 day before

        Appointment appt = Appointment.builder()
                .id(601L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.of(2026, 10, 3))
                .status(AppointmentStatus.COMPLETED)
                .followUpDate(targetFollowUpDate)
                .followUpTime(LocalTime.of(10, 0))
                .followUpReminderSent(false)
                .build();

        List<AppointmentStatus> excluded = Collections.singletonList(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findByFollowUpDateAndStatusNotInAndFollowUpReminderSentFalse(targetFollowUpDate, excluded))
                .thenReturn(Collections.singletonList(appt));

        int sent = appointmentService.sendFollowUpReminders(today, 1);

        assertEquals(1, sent);
        assertTrue(appt.isFollowUpReminderSent());
        verify(notificationService, times(1)).createFollowUpReminder(appt);
        verify(appointmentRepository, times(1)).save(appt);
    }

    @Test
    @DisplayName("8. Reminder is sent one day before follow-up date")
    void test8_ReminderSentOneDayBefore() {
        LocalDate referenceDate = LocalDate.of(2026, 10, 9);
        LocalDate followUpDate = LocalDate.of(2026, 10, 10);

        Appointment appt = Appointment.builder()
                .id(602L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.of(2026, 10, 1))
                .status(AppointmentStatus.COMPLETED)
                .followUpDate(followUpDate)
                .followUpTime(LocalTime.of(14, 0))
                .followUpNotes("Review lab results")
                .followUpReminderSent(false)
                .build();

        List<AppointmentStatus> excluded = Collections.singletonList(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findByFollowUpDateAndStatusNotInAndFollowUpReminderSentFalse(followUpDate, excluded))
                .thenReturn(Collections.singletonList(appt));

        int sent = appointmentService.sendFollowUpReminders(referenceDate, 1);

        assertEquals(1, sent);
        assertTrue(appt.isFollowUpReminderSent());
        verify(notificationService, times(1)).createFollowUpReminder(appt);
    }

    @Test
    @DisplayName("9. Reminder is not sent multiple times (Duplicate prevention)")
    void test9_ReminderNotSentMultipleTimes_DuplicatePrevention() {
        LocalDate referenceDate = LocalDate.of(2026, 10, 9);
        LocalDate followUpDate = LocalDate.of(2026, 10, 10);

        Appointment appt = Appointment.builder()
                .id(603L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.of(2026, 10, 1))
                .status(AppointmentStatus.COMPLETED)
                .followUpDate(followUpDate)
                .followUpTime(LocalTime.of(10, 0))
                .followUpReminderSent(true) // Already marked as sent
                .build();

        List<AppointmentStatus> excluded = Collections.singletonList(AppointmentStatus.CANCELLED);
        // Repository returns empty list because followUpReminderSent is already true
        when(appointmentRepository.findByFollowUpDateAndStatusNotInAndFollowUpReminderSentFalse(followUpDate, excluded))
                .thenReturn(Collections.emptyList());

        int sent = appointmentService.sendFollowUpReminders(referenceDate, 1);

        assertEquals(0, sent);
        verify(notificationService, never()).createFollowUpReminder(any(Appointment.class));
    }

    @Test
    @DisplayName("10. Cancelled appointment does not receive follow-up reminder")
    void test10_CancelledAppointment_DoesNotReceiveReminder() {
        LocalDate referenceDate = LocalDate.of(2026, 10, 9);
        LocalDate followUpDate = LocalDate.of(2026, 10, 10);

        Appointment cancelledAppt = Appointment.builder()
                .id(604L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.of(2026, 10, 1))
                .status(AppointmentStatus.CANCELLED)
                .followUpDate(followUpDate)
                .followUpReminderSent(false)
                .build();

        List<AppointmentStatus> excluded = Collections.singletonList(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findByFollowUpDateAndStatusNotInAndFollowUpReminderSentFalse(followUpDate, excluded))
                .thenReturn(Collections.singletonList(cancelledAppt));

        int sent = appointmentService.sendFollowUpReminders(referenceDate, 1);

        assertEquals(0, sent);
        verify(notificationService, never()).createFollowUpReminder(any(Appointment.class));
    }

    @Test
    @DisplayName("11. Completed follow-up / past appointment does not receive future reminders")
    void test11_CompletedFollowUp_DoesNotReceiveFutureReminder() {
        LocalDate today = LocalDate.of(2026, 10, 15);
        LocalDate pastFollowUpDate = LocalDate.of(2026, 10, 10); // In past

        List<AppointmentStatus> excluded = Collections.singletonList(AppointmentStatus.CANCELLED);
        LocalDate scanDate = today.plusDays(1); // scans for 2026-10-16
        when(appointmentRepository.findByFollowUpDateAndStatusNotInAndFollowUpReminderSentFalse(scanDate, excluded))
                .thenReturn(Collections.emptyList());

        int sent = appointmentService.sendFollowUpReminders(today, 1);

        assertEquals(0, sent);
        verify(notificationService, never()).createFollowUpReminder(any(Appointment.class));
    }

    @Test
    @DisplayName("12. Patient receives the correct notification with Doctor name and time")
    void test12_PatientReceivesCorrectNotification() {
        NotificationRepository mockNotifRepo = mock(NotificationRepository.class);
        com.example.healthcareappointmentmanagementsystem.mapper.NotificationMapper mockMapper = mock(com.example.healthcareappointmentmanagementsystem.mapper.NotificationMapper.class);
        NotificationServiceImpl notificationServiceImpl = new NotificationServiceImpl(mockNotifRepo, mockMapper);

        Appointment appt = Appointment.builder()
                .id(701L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.of(2026, 10, 3))
                .status(AppointmentStatus.COMPLETED)
                .followUpDate(LocalDate.of(2026, 10, 10))
                .followUpTime(LocalTime.of(10, 0))
                .followUpNotes("Review blood pressure and continue medication.")
                .build();

        when(mockNotifRepo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification notif = notificationServiceImpl.createFollowUpReminder(appt);

        assertNotNull(notif);
        assertEquals(patient1, notif.getPatient());
        assertEquals("FOLLOW_UP_REMINDER", notif.getNotificationType());
        assertEquals("🔔 Follow-up Reminder", notif.getTitle());
        assertTrue(notif.getMessage().contains("Dr. Priya Nair"));
        assertTrue(notif.getMessage().contains("10:00 AM") || notif.getMessage().contains("10:00"));
        assertTrue(notif.getMessage().contains("October 10, 2026") || notif.getMessage().contains("2026-10-10") || notif.getMessage().contains("tomorrow"));
        assertFalse(notif.isRead());
        assertNotNull(notif.getAppointment());
        assertEquals(701L, notif.getAppointment().getId());
    }

    @Test
    @DisplayName("13. Multiple patients receive their own correct reminders")
    void test13_MultiplePatientsReceiveTheirOwnCorrectReminders() {
        LocalDate referenceDate = LocalDate.of(2026, 10, 9);
        LocalDate targetDate = LocalDate.of(2026, 10, 10);

        Appointment appt1 = Appointment.builder()
                .id(801L)
                .doctor(doctor1)
                .patient(patient1)
                .status(AppointmentStatus.COMPLETED)
                .followUpDate(targetDate)
                .followUpTime(LocalTime.of(9, 30))
                .followUpReminderSent(false)
                .build();

        Appointment appt2 = Appointment.builder()
                .id(802L)
                .doctor(doctor2)
                .patient(patient2)
                .status(AppointmentStatus.COMPLETED)
                .followUpDate(targetDate)
                .followUpTime(LocalTime.of(11, 0))
                .followUpReminderSent(false)
                .build();

        List<AppointmentStatus> excluded = Collections.singletonList(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findByFollowUpDateAndStatusNotInAndFollowUpReminderSentFalse(targetDate, excluded))
                .thenReturn(Arrays.asList(appt1, appt2));

        int sent = appointmentService.sendFollowUpReminders(referenceDate, 1);

        assertEquals(2, sent);
        assertTrue(appt1.isFollowUpReminderSent());
        assertTrue(appt2.isFollowUpReminderSent());

        verify(notificationService, times(1)).createFollowUpReminder(appt1);
        verify(notificationService, times(1)).createFollowUpReminder(appt2);
        verify(appointmentRepository, times(1)).save(appt1);
        verify(appointmentRepository, times(1)).save(appt2);
    }

    @Test
    @DisplayName("14. Appointment with no follow-up works normally without error")
    void test14_NoFollowUpAppointmentWorksNormally() {
        authenticateAsPatient(patientUser1);

        Appointment appt = Appointment.builder()
                .id(901L)
                .doctor(doctor1)
                .patient(patient1)
                .appointmentDate(LocalDate.now().minusDays(1))
                .status(AppointmentStatus.COMPLETED)
                .followUpDate(null)
                .followUpTime(null)
                .followUpNotes(null)
                .build();

        when(appointmentRepository.findById(901L)).thenReturn(Optional.of(appt));

        FollowUpResponse response = appointmentService.getAppointmentFollowUp(901L, patientUser1.getEmail());

        assertNotNull(response);
        assertEquals(901L, response.getAppointmentId());
        assertNull(response.getFollowUpDate());
        assertNull(response.getFollowUpTime());
        assertNull(response.getFollowUpNotes());
        assertEquals("No follow-up scheduled.", response.getMessage());
    }

    @Test
    @DisplayName("15. Scheduler trigger via FollowUpReminderScheduler invokes service")
    void test15_FollowUpReminderSchedulerTrigger() {
        AppointmentService mockApptService = mock(AppointmentService.class);
        FollowUpReminderScheduler scheduler = new FollowUpReminderScheduler(mockApptService);

        when(mockApptService.sendFollowUpReminders()).thenReturn(3);

        scheduler.scheduleFollowUpReminders();

        verify(mockApptService, times(1)).sendFollowUpReminders();
    }
}
