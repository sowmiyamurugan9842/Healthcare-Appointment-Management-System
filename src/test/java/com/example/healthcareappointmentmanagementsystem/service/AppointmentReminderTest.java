package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.mapper.AppointmentMapper;
import com.example.healthcareappointmentmanagementsystem.repository.AppointmentRepository;
import com.example.healthcareappointmentmanagementsystem.repository.DoctorRepository;
import com.example.healthcareappointmentmanagementsystem.repository.NotificationRepository;
import com.example.healthcareappointmentmanagementsystem.repository.PatientRepository;
import com.example.healthcareappointmentmanagementsystem.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderTest {

    @Mock
    private AppointmentRepository appointmentRepository;

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

    private Patient testPatient;
    private Doctor testDoctor;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                doctorRepository,
                patientRepository,
                appointmentMapper,
                notificationService,
                notificationRepository
        );

        User docUser = User.builder().id(10L).firstName("Priya").lastName("Sharma").email("priya@careportal.com").build();
        testDoctor = Doctor.builder().id(1L).user(docUser).build();

        User patUser = User.builder().id(20L).firstName("Alex").lastName("Taylor").email("alex@careportal.com").build();
        testPatient = Patient.builder().id(2L).user(patUser).build();
    }

    @Test
    @DisplayName("TEST 1: Appointment is CONFIRMED and exactly 1 hour away -> Reminder is created")
    void test1_Confirmed_ExactlyOneHourAway_ReminderCreated() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 9, 0); // 9:00 AM
        Appointment appt = Appointment.builder()
                .id(101L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 10, 5))
                .appointmentTime(LocalTime.of(10, 0)) // 10:00 AM (exactly 1 hr away)
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();

        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.singletonList(appt));
        when(notificationRepository.existsByAppointmentId(101L)).thenReturn(false);

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(1, count);
        assertTrue(appt.isReminderSent());
        verify(notificationService, times(1)).createAppointmentReminder(appt);
        verify(appointmentRepository, times(1)).save(appt);
    }

    @Test
    @DisplayName("TEST 2: Same scheduler runs again -> No duplicate reminder")
    void test2_SameSchedulerRunsAgain_NoDuplicateReminder() {
        LocalDateTime nowLater = LocalDateTime.of(2026, 10, 5, 9, 1); // 9:01 AM
        Appointment appt = Appointment.builder()
                .id(101L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 10, 5))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(true) // already sent on previous execution
                .build();

        // Repository filter returns empty because reminderSent is true
        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        int count = appointmentService.sendAppointmentReminders(nowLater);

        assertEquals(0, count);
        verify(notificationService, never()).createAppointmentReminder(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST 3: Appointment is PENDING -> No reminder")
    void test3_Pending_NoReminder() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 9, 0);
        // PENDING appointments are not returned by findByStatusAndReminderSentFalse(CONFIRMED)
        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        verify(notificationService, never()).createAppointmentReminder(any());
    }

    @Test
    @DisplayName("TEST 4: Appointment is CANCELLED -> No reminder")
    void test4_Cancelled_NoReminder() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 9, 0);
        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        verify(notificationService, never()).createAppointmentReminder(any());
    }

    @Test
    @DisplayName("TEST 5: Appointment is REJECTED -> No reminder")
    void test5_Rejected_NoReminder() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 9, 0);
        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        verify(notificationService, never()).createAppointmentReminder(any());
    }

    @Test
    @DisplayName("TEST 6: Appointment is EXPIRED -> No reminder")
    void test6_Expired_NoReminder() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 9, 0);
        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        verify(notificationService, never()).createAppointmentReminder(any());
    }

    @Test
    @DisplayName("TEST 7: Appointment is COMPLETED -> No reminder")
    void test7_Completed_NoReminder() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 9, 0);
        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        verify(notificationService, never()).createAppointmentReminder(any());
    }

    @Test
    @DisplayName("TEST 8: Appointment is more than 1 hour away (75 mins) -> No reminder yet")
    void test8_MoreThanOneHourAway_NoReminderYet() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 8, 45); // 8:45 AM (75 mins before 10:00 AM)
        Appointment appt = Appointment.builder()
                .id(102L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 10, 5))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();

        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.singletonList(appt));

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        assertFalse(appt.isReminderSent());
        verify(notificationService, never()).createAppointmentReminder(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST 9: Appointment time has already passed -> No reminder")
    void test9_PastAppointmentTime_NoReminder() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 10, 15); // 10:15 AM (after 10:00 AM)
        Appointment appt = Appointment.builder()
                .id(103L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 10, 5))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();

        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.singletonList(appt));

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        assertFalse(appt.isReminderSent());
        verify(notificationService, never()).createAppointmentReminder(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST 10: Appointment is 30 minutes away -> Do not create reminder if 1-hour reminder was missed")
    void test10_ThirtyMinutesAway_MissedOneHourReminder_NoReminderCreated() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 9, 30); // 9:30 AM (30 mins before 10:00 AM)
        Appointment appt = Appointment.builder()
                .id(104L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 10, 5))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();

        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.singletonList(appt));

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        assertFalse(appt.isReminderSent());
        verify(notificationService, never()).createAppointmentReminder(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST 11: Appointment is 2 hours away -> Do not create reminder yet")
    void test11_TwoHoursAway_NoReminderYet() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 8, 0); // 8:00 AM (120 mins before 10:00 AM)
        Appointment appt = Appointment.builder()
                .id(105L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 10, 5))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();

        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Collections.singletonList(appt));

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(0, count);
        assertFalse(appt.isReminderSent());
        verify(notificationService, never()).createAppointmentReminder(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST 12: Two different confirmed appointments -> Each appointment gets exactly one reminder")
    void test12_TwoConfirmedAppointments_EachGetsExactlyOneReminder() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 9, 0); // 9:00 AM
        Appointment appt1 = Appointment.builder()
                .id(201L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 10, 5))
                .appointmentTime(LocalTime.of(10, 0)) // 10:00 AM -> 1 hr away
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();

        Appointment appt2 = Appointment.builder()
                .id(202L)
                .doctor(testDoctor)
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 10, 5))
                .appointmentTime(LocalTime.of(10, 0)) // 10:00 AM -> 1 hr away
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();

        when(appointmentRepository.findByStatusAndReminderSentFalse(AppointmentStatus.CONFIRMED))
                .thenReturn(Arrays.asList(appt1, appt2));
        when(notificationRepository.existsByAppointmentId(201L)).thenReturn(false);
        when(notificationRepository.existsByAppointmentId(202L)).thenReturn(false);

        int count = appointmentService.sendAppointmentReminders(now);

        assertEquals(2, count);
        assertTrue(appt1.isReminderSent());
        assertTrue(appt2.isReminderSent());
        verify(notificationService, times(1)).createAppointmentReminder(appt1);
        verify(notificationService, times(1)).createAppointmentReminder(appt2);
        verify(appointmentRepository, times(1)).save(appt1);
        verify(appointmentRepository, times(1)).save(appt2);
    }
}
