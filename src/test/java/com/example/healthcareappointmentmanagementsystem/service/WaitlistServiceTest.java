package com.example.healthcareappointmentmanagementsystem.service;

import com.example.healthcareappointmentmanagementsystem.dto.request.AppointmentRequest;
import com.example.healthcareappointmentmanagementsystem.dto.request.WaitlistRequest;
import com.example.healthcareappointmentmanagementsystem.dto.response.AppointmentResponse;
import com.example.healthcareappointmentmanagementsystem.dto.response.WaitlistResponse;
import com.example.healthcareappointmentmanagementsystem.entity.*;
import com.example.healthcareappointmentmanagementsystem.exception.BadRequestException;
import com.example.healthcareappointmentmanagementsystem.exception.UnauthorizedException;
import com.example.healthcareappointmentmanagementsystem.mapper.WaitlistMapper;
import com.example.healthcareappointmentmanagementsystem.repository.*;
import com.example.healthcareappointmentmanagementsystem.service.impl.WaitlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WaitlistServiceTest {

    @Mock
    private WaitlistRepository waitlistRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WaitlistMapper waitlistMapper;

    @InjectMocks
    private WaitlistServiceImpl waitlistService;

    private User patientUser;
    private Patient patient;
    private User doctorUser;
    private Doctor doctor;
    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .departmentName("Cardiology")
                .description("Heart and vascular care")
                .build();

        patientUser = User.builder()
                .id(10L)
                .email("patient.test@careportal.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.PATIENT)
                .phoneNumber("+1-555-0100")
                .build();

        patient = Patient.builder()
                .id(4L)
                .user(patientUser)
                .build();

        doctorUser = User.builder()
                .id(20L)
                .email("doctor.smith@careportal.com")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.DOCTOR)
                .build();

        doctor = Doctor.builder()
                .id(1L)
                .user(doctorUser)
                .department(department)
                .specialization("Cardiologist")
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .consultationFee(100.0)
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "patient.test@careportal.com", null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(userRepository.findByEmail("patient.test@careportal.com")).thenReturn(Optional.of(patientUser));
        lenient().when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patient));
        lenient().when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
    }

    // 1. Patient joins waitlist successfully
    @Test
    @DisplayName("1. Patient joins waitlist successfully")
    void testJoinWaitlist_Success() {
        WaitlistRequest request = WaitlistRequest.builder()
                .doctorId(1L)
                .appointmentDate(LocalDate.now().plusDays(2))
                .preferredTime(LocalTime.of(10, 30))
                .reasonForVisit("Cardiology routine consultation")
                .build();

        WaitlistEntry savedEntry = WaitlistEntry.builder()
                .id(101L)
                .doctor(doctor)
                .patient(patient)
                .appointmentDate(LocalDate.now().plusDays(2))
                .preferredTime(LocalTime.of(10, 30))
                .status(WaitlistStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();

        WaitlistResponse responseDto = WaitlistResponse.builder()
                .id(101L)
                .doctorId(1L)
                .doctorName("Dr. Alice Smith")
                .patientId(4L)
                .patientName("John Doe")
                .appointmentDate(LocalDate.now().plusDays(2))
                .preferredTime(LocalTime.of(10, 30))
                .status(WaitlistStatus.WAITING)
                .queuePosition(1)
                .build();

        when(waitlistRepository.existsByPatientAndDoctorAndAppointmentDateAndStatusIn(eq(patient), eq(doctor), any(LocalDate.class), anyCollection()))
                .thenReturn(false);
        when(appointmentRepository.findByPatientAndAppointmentDate(eq(patient), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(waitlistMapper.toEntity(any(WaitlistRequest.class), eq(doctor), eq(patient)))
                .thenReturn(savedEntry);
        when(waitlistRepository.save(any(WaitlistEntry.class)))
                .thenReturn(savedEntry);
        when(waitlistRepository.countByDoctorAndAppointmentDateAndStatusAndCreatedAtBefore(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.WAITING), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(waitlistMapper.toResponse(eq(savedEntry), eq(1)))
                .thenReturn(responseDto);

        WaitlistResponse result = waitlistService.joinWaitlist(request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(WaitlistStatus.WAITING, result.getStatus());
        assertEquals(1, result.getQueuePosition());
        verify(waitlistRepository).save(any(WaitlistEntry.class));
    }

    // 2. Duplicate waitlist entry rejected
    @Test
    @DisplayName("2. Duplicate waitlist entry rejected with clear error")
    void testJoinWaitlist_DuplicateRejected() {
        WaitlistRequest request = WaitlistRequest.builder()
                .doctorId(1L)
                .appointmentDate(LocalDate.now().plusDays(2))
                .reasonForVisit("Followup consultation")
                .build();

        when(waitlistRepository.existsByPatientAndDoctorAndAppointmentDateAndStatusIn(eq(patient), eq(doctor), any(LocalDate.class), anyCollection()))
                .thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> waitlistService.joinWaitlist(request));
        assertTrue(ex.getMessage().contains("already on the waitlist"));
        verify(waitlistRepository, never()).save(any(WaitlistEntry.class));
    }

    // 3. Patient views own waitlist entries
    @Test
    @DisplayName("3. Patient views own waitlist entries")
    void testGetMyWaitlist_Success() {
        WaitlistEntry entry1 = WaitlistEntry.builder()
                .id(101L)
                .doctor(doctor)
                .patient(patient)
                .appointmentDate(LocalDate.now().plusDays(1))
                .status(WaitlistStatus.WAITING)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        WaitlistResponse resp1 = WaitlistResponse.builder()
                .id(101L)
                .status(WaitlistStatus.WAITING)
                .queuePosition(1)
                .build();

        when(waitlistRepository.findByPatientOrderByCreatedAtDesc(patient)).thenReturn(List.of(entry1));
        when(waitlistMapper.toResponse(eq(entry1), anyInt())).thenReturn(resp1);

        List<WaitlistResponse> list = waitlistService.getMyWaitlist();
        assertEquals(1, list.size());
        assertEquals(101L, list.get(0).getId());
    }

    // 4. Patient cancels waitlist entry
    @Test
    @DisplayName("4. Patient cancels active waitlist entry")
    void testCancelWaitlist_Success() {
        WaitlistEntry entry = WaitlistEntry.builder()
                .id(101L)
                .doctor(doctor)
                .patient(patient)
                .status(WaitlistStatus.WAITING)
                .build();

        WaitlistResponse responseDto = WaitlistResponse.builder()
                .id(101L)
                .status(WaitlistStatus.CANCELLED)
                .build();

        when(waitlistRepository.findById(101L)).thenReturn(Optional.of(entry));
        when(waitlistRepository.save(entry)).thenReturn(entry);
        when(waitlistMapper.toResponse(entry, null)).thenReturn(responseDto);

        WaitlistResponse cancelled = waitlistService.cancelWaitlistEntry(101L);
        assertNotNull(cancelled);
        assertEquals(WaitlistStatus.CANCELLED, cancelled.getStatus());
        assertEquals(WaitlistStatus.CANCELLED, entry.getStatus());
    }

    // 5. Correct patient selected from queue (FIFO fair queue)
    @Test
    @DisplayName("5. Correct patient selected from queue in FIFO order")
    void testProcessWaitlist_FifoOrdering() {
        User user2 = User.builder().id(11L).firstName("Priya").lastName("Kumar").email("priya@test.com").build();
        Patient patient2 = Patient.builder().id(5L).user(user2).build();

        LocalDateTime t1 = LocalDateTime.now().minusHours(2);
        LocalDateTime t2 = LocalDateTime.now().minusHours(1);

        WaitlistEntry entry1 = WaitlistEntry.builder()
                .id(1L).patient(patient).doctor(doctor).appointmentDate(LocalDate.now().plusDays(1))
                .status(WaitlistStatus.WAITING).createdAt(t1).build();

        WaitlistEntry entry2 = WaitlistEntry.builder()
                .id(2L).patient(patient2).doctor(doctor).appointmentDate(LocalDate.now().plusDays(1))
                .status(WaitlistStatus.WAITING).createdAt(t2).build();

        when(waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.NOTIFIED)))
                .thenReturn(Collections.emptyList());
        when(waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.WAITING)))
                .thenReturn(List.of(entry1, entry2));

        int notifiedCount = waitlistService.processWaitlistForSlot(doctor, LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        assertEquals(1, notifiedCount);
        assertEquals(WaitlistStatus.NOTIFIED, entry1.getStatus());
        assertEquals(LocalTime.of(10, 0), entry1.getOfferedTime());
        assertEquals(WaitlistStatus.WAITING, entry2.getStatus()); // Second patient still waiting
        verify(notificationService).createWaitlistOfferNotification(eq(patient), eq(doctor), any(LocalDate.class), eq(LocalTime.of(10, 0)), anyInt());
    }

    // 6. Preferred time matching takes priority over any-time preference
    @Test
    @DisplayName("6. Preferred time matching prioritizes exact slot preference")
    void testProcessWaitlist_PreferredTimePriority() {
        User user2 = User.builder().id(11L).firstName("Ravi").email("ravi@test.com").build();
        Patient patient2 = Patient.builder().id(5L).user(user2).build();

        // Patient 1 joined earlier with ANY TIME (preferredTime = null)
        WaitlistEntry entry1AnyTime = WaitlistEntry.builder()
                .id(1L).patient(patient).doctor(doctor).appointmentDate(LocalDate.now().plusDays(1))
                .preferredTime(null).status(WaitlistStatus.WAITING).createdAt(LocalDateTime.now().minusHours(3)).build();

        // Patient 2 joined later with EXACT MATCH (11:30)
        WaitlistEntry entry2ExactMatch = WaitlistEntry.builder()
                .id(2L).patient(patient2).doctor(doctor).appointmentDate(LocalDate.now().plusDays(1))
                .preferredTime(LocalTime.of(11, 30)).status(WaitlistStatus.WAITING).createdAt(LocalDateTime.now().minusHours(1)).build();

        when(waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.NOTIFIED)))
                .thenReturn(Collections.emptyList());
        when(waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.WAITING)))
                .thenReturn(List.of(entry1AnyTime, entry2ExactMatch));

        int notifiedCount = waitlistService.processWaitlistForSlot(doctor, LocalDate.now().plusDays(1), LocalTime.of(11, 30));

        assertEquals(1, notifiedCount);
        assertEquals(WaitlistStatus.NOTIFIED, entry2ExactMatch.getStatus());
        assertEquals(LocalTime.of(11, 30), entry2ExactMatch.getOfferedTime());
        verify(notificationService).createWaitlistOfferNotification(eq(patient2), eq(doctor), any(LocalDate.class), eq(LocalTime.of(11, 30)), anyInt());
    }

    // 7. Any-time patient matching when no exact preference exists
    @Test
    @DisplayName("7. Any-time patient receives slot when no exact preferred time match exists")
    void testProcessWaitlist_AnyTimeFallback() {
        WaitlistEntry entryAnyTime = WaitlistEntry.builder()
                .id(1L).patient(patient).doctor(doctor).appointmentDate(LocalDate.now().plusDays(1))
                .preferredTime(null).status(WaitlistStatus.WAITING).createdAt(LocalDateTime.now().minusHours(2)).build();

        when(waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.NOTIFIED)))
                .thenReturn(Collections.emptyList());
        when(waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.WAITING)))
                .thenReturn(List.of(entryAnyTime));

        int notifiedCount = waitlistService.processWaitlistForSlot(doctor, LocalDate.now().plusDays(1), LocalTime.of(14, 0));

        assertEquals(1, notifiedCount);
        assertEquals(WaitlistStatus.NOTIFIED, entryAnyTime.getStatus());
        assertEquals(LocalTime.of(14, 0), entryAnyTime.getOfferedTime());
    }

    // 8. Patient confirms offered slot and official appointment is created
    @Test
    @DisplayName("8. Patient confirms offered slot and appointment is booked")
    void testConfirmOfferedSlot_Success() {
        WaitlistEntry entry = WaitlistEntry.builder()
                .id(101L)
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(LocalDate.now().plusDays(2))
                .offeredTime(LocalTime.of(10, 0))
                .reasonForVisit("Cardiology visit")
                .status(WaitlistStatus.NOTIFIED)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        AppointmentResponse apptResponse = AppointmentResponse.builder()
                .id(501L)
                .doctorName("Dr. Alice Smith")
                .patientName("John Doe")
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime(LocalTime.of(10, 0))
                .status(AppointmentStatus.PENDING)
                .build();

        when(waitlistRepository.findById(101L)).thenReturn(Optional.of(entry));
        when(appointmentService.bookAppointment(any(AppointmentRequest.class))).thenReturn(apptResponse);
        when(waitlistRepository.save(entry)).thenReturn(entry);

        AppointmentResponse result = waitlistService.confirmOfferedSlot(101L);

        assertNotNull(result);
        assertEquals(501L, result.getId());
        assertEquals(WaitlistStatus.BOOKED, entry.getStatus());
        verify(appointmentService).bookAppointment(any(AppointmentRequest.class));
        verify(waitlistRepository).save(entry);
    }

    // 9. Expired offer cannot be confirmed and is reallocated
    @Test
    @DisplayName("9. Expired slot offer throws error and reallocates slot")
    void testConfirmOfferedSlot_ExpiredOfferThrows() {
        WaitlistEntry entry = WaitlistEntry.builder()
                .id(101L)
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(LocalDate.now().plusDays(2))
                .offeredTime(LocalTime.of(10, 0))
                .status(WaitlistStatus.NOTIFIED)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired 5 mins ago
                .build();

        when(waitlistRepository.findById(101L)).thenReturn(Optional.of(entry));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> waitlistService.confirmOfferedSlot(101L));
        assertTrue(ex.getMessage().contains("expired"));
        assertEquals(WaitlistStatus.EXPIRED, entry.getStatus());
        verify(appointmentService, never()).bookAppointment(any(AppointmentRequest.class));
    }

    // 10. Automatic offer expiry scan reallocates to next patient
    @Test
    @DisplayName("10. Check and expire unaccepted offers promotes next patient")
    void testCheckAndExpirePendingOffers_PromotesNextPatient() {
        WaitlistEntry expiredEntry = WaitlistEntry.builder()
                .id(101L)
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(LocalDate.now().plusDays(2))
                .offeredTime(LocalTime.of(10, 0))
                .status(WaitlistStatus.NOTIFIED)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        User user2 = User.builder().id(12L).firstName("Arun").email("arun@test.com").build();
        Patient patient2 = Patient.builder().id(6L).user(user2).build();
        WaitlistEntry nextEntry = WaitlistEntry.builder()
                .id(102L)
                .patient(patient2)
                .doctor(doctor)
                .appointmentDate(LocalDate.now().plusDays(2))
                .status(WaitlistStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();

        when(waitlistRepository.findByStatusAndExpiresAtBefore(eq(WaitlistStatus.NOTIFIED), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredEntry));
        when(waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.NOTIFIED)))
                .thenReturn(Collections.emptyList());
        when(waitlistRepository.findByDoctorAndAppointmentDateAndStatusOrderByCreatedAtAsc(eq(doctor), any(LocalDate.class), eq(WaitlistStatus.WAITING)))
                .thenReturn(List.of(nextEntry));

        int reallocated = waitlistService.checkAndExpirePendingOffers();

        assertEquals(1, reallocated);
        assertEquals(WaitlistStatus.EXPIRED, expiredEntry.getStatus());
        assertEquals(WaitlistStatus.NOTIFIED, nextEntry.getStatus());
        assertEquals(LocalTime.of(10, 0), nextEntry.getOfferedTime());
    }

    // 11. Unauthorized patient cannot access another patient's waitlist
    @Test
    @DisplayName("11. Unauthorized patient cannot cancel another patient's waitlist")
    void testCancelWaitlist_UnauthorizedAccess() {
        User otherUser = User.builder().id(99L).email("other@careportal.com").build();
        Patient otherPatient = Patient.builder().id(99L).user(otherUser).build();

        WaitlistEntry otherEntry = WaitlistEntry.builder()
                .id(202L)
                .patient(otherPatient)
                .doctor(doctor)
                .status(WaitlistStatus.WAITING)
                .build();

        when(waitlistRepository.findById(202L)).thenReturn(Optional.of(otherEntry));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> waitlistService.cancelWaitlistEntry(202L));
        assertTrue(ex.getMessage().contains("not authorized"));
    }
}
