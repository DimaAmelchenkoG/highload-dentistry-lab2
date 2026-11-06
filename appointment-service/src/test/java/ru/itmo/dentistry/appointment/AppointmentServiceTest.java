package ru.itmo.dentistry.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.itmo.dentistry.appointment.catalogclient.CatalogGateway;
import ru.itmo.dentistry.appointment.catalogclient.CatalogProcedureView;
import ru.itmo.dentistry.appointment.catalogclient.CatalogUserView;
import ru.itmo.dentistry.appointment.common.BusinessValidationException;
import ru.itmo.dentistry.appointment.common.ConflictException;
import ru.itmo.dentistry.appointment.notification.AfterCommitNotificationPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentProcedureRepository appointmentProcedureRepository;

    @Mock
    private CatalogGateway catalog;

    @Mock
    private AfterCommitNotificationPublisher notifications;

    @Mock
    private EntityManager entityManager;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        Query query = org.mockito.Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
        service = new AppointmentService(
                appointmentRepository,
                appointmentProcedureRepository,
                catalog,
                notifications,
                new AppointmentCursorCodec(),
                entityManager,
                MOSCOW,
                30);
    }

    @Test
    void rejectsSamePatientAndDoctor() {
        UUID id = UUID.randomUUID();
        var request = new CreateAppointmentRequest(id, id, Instant.parse("2026-11-06T07:00:00Z"),
                Instant.parse("2026-11-06T07:30:00Z"), null);

        assertThrows(BusinessValidationException.class, () -> service.create(request));
    }

    @Test
    void rejectsInactiveParticipants() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        when(catalog.user(patientId)).thenReturn(user(patientId, "PATIENT", false));
        when(catalog.user(doctorId)).thenReturn(doctor(doctorId));

        var request = new CreateAppointmentRequest(patientId, doctorId, Instant.parse("2026-11-06T07:00:00Z"),
                Instant.parse("2026-11-06T07:30:00Z"), "  checkup  ");

        BusinessValidationException error = assertThrows(BusinessValidationException.class, () -> service.create(request));
        assertEquals("Patient and doctor must be active", error.getMessage());
    }

    @Test
    void rejectsSlotOutsideWorkingHours() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        when(catalog.user(patientId)).thenReturn(user(patientId, "PATIENT", true));
        when(catalog.user(doctorId)).thenReturn(doctor(doctorId));

        var request = new CreateAppointmentRequest(patientId, doctorId, Instant.parse("2026-11-06T04:00:00Z"),
                Instant.parse("2026-11-06T04:30:00Z"), null);

        assertThrows(BusinessValidationException.class, () -> service.create(request));
    }

    @Test
    void rejectsOverlappingDoctor() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        when(catalog.user(patientId)).thenReturn(user(patientId, "PATIENT", true));
        when(catalog.user(doctorId)).thenReturn(doctor(doctorId));
        when(appointmentRepository.hasDoctorOverlap(eq(doctorId), any(), any(), eq(null))).thenReturn(true);

        var request = new CreateAppointmentRequest(patientId, doctorId, Instant.parse("2026-11-06T07:00:00Z"),
                Instant.parse("2026-11-06T07:30:00Z"), null);

        assertThrows(ConflictException.class, () -> service.create(request));
    }

    @Test
    void createsAppointmentInsideWorkingHours() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        when(catalog.user(patientId)).thenReturn(user(patientId, "PATIENT", true));
        when(catalog.user(doctorId)).thenReturn(doctor(doctorId));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateAppointmentRequest(patientId, doctorId, Instant.parse("2026-11-06T07:00:00Z"),
                Instant.parse("2026-11-06T07:30:00Z"), "  checkup  ");

        AppointmentResponse response = service.create(request);

        assertEquals("Ivan Petrov", response.patientName());
        assertEquals("Anna Smirnova", response.doctorName());
        assertEquals(AppointmentStatus.SCHEDULED, response.status());
        assertEquals("checkup", response.comment());
        verify(notifications).created(response.id());
    }

    @Test
    void completedAppointmentCannotBeCancelled() {
        UUID id = UUID.randomUUID();
        Appointment appointment = appointment(id, AppointmentStatus.COMPLETED);
        when(appointmentRepository.findLockedById(id)).thenReturn(Optional.of(appointment));

        assertThrows(ConflictException.class, () -> service.cancel(id));
    }

    @Test
    void completesScheduledAppointment() {
        UUID id = UUID.randomUUID();
        Appointment appointment = appointment(id, AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findLockedById(id)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.complete(id);

        assertEquals(AppointmentStatus.COMPLETED, response.status());
    }

    @Test
    void rejectsProcedureTheDoctorDoesNotPerform() {
        UUID appointmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId, AppointmentStatus.SCHEDULED);
        appointment.setDoctorId(doctorId);
        when(appointmentRepository.findLockedById(appointmentId)).thenReturn(Optional.of(appointment));
        when(catalog.procedure(procedureId))
                .thenReturn(new CatalogProcedureView(procedureId, "Cleaning", new BigDecimal("1500.00"), true));
        when(catalog.user(doctorId)).thenReturn(doctor(doctorId));

        var request = new AppointmentProcedureRequest(procedureId, 1, 11);

        BusinessValidationException error = assertThrows(
                BusinessValidationException.class, () -> service.addProcedure(appointmentId, request));
        assertEquals("The doctor does not perform this procedure", error.getMessage());
    }

    @Test
    void availabilitySkipsOccupiedSlot() {
        UUID doctorId = UUID.randomUUID();
        when(catalog.user(doctorId)).thenReturn(doctor(doctorId));
        Appointment occupied = new Appointment();
        occupied.setStartsAt(Instant.parse("2026-11-06T06:00:00Z"));
        occupied.setEndsAt(Instant.parse("2026-11-06T06:30:00Z"));
        when(appointmentRepository.findByDoctorIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAt(
                eq(doctorId), eq(AppointmentStatus.SCHEDULED), any(), any()))
                .thenReturn(List.of(occupied));

        List<AvailabilityResponse> slots = service.availability(doctorId, LocalDate.of(2026, 11, 6));

        assertTrue(slots.stream().noneMatch(slot -> slot.startsAt().equals(Instant.parse("2026-11-06T06:00:00Z"))));
        assertTrue(slots.stream().anyMatch(slot -> slot.startsAt().equals(Instant.parse("2026-11-06T06:30:00Z"))));
        assertEquals(17, slots.size());
    }

    private static CatalogUserView user(UUID id, String role, boolean active) {
        return new CatalogUserView(id, "Ivan Petrov", role, active, null, null, Set.of());
    }

    private static CatalogUserView doctor(UUID id) {
        return new CatalogUserView(
                id,
                "Anna Smirnova",
                "DOCTOR",
                true,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                Set.of());
    }

    private static Appointment appointment(UUID id, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        org.springframework.test.util.ReflectionTestUtils.setField(appointment, "id", id);
        appointment.setPatientId(UUID.randomUUID());
        appointment.setPatientName("Ivan Petrov");
        appointment.setDoctorId(UUID.randomUUID());
        appointment.setDoctorName("Anna Smirnova");
        appointment.setStartsAt(Instant.parse("2026-11-06T07:00:00Z"));
        appointment.setEndsAt(Instant.parse("2026-11-06T07:30:00Z"));
        appointment.setStatus(status);
        return appointment;
    }
}
