package ru.itmo.dentistry.appointment;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.dentistry.appointment.catalogclient.CatalogGateway;
import ru.itmo.dentistry.appointment.catalogclient.CatalogUserView;
import ru.itmo.dentistry.appointment.common.BusinessValidationException;
import ru.itmo.dentistry.appointment.common.ConflictException;
import ru.itmo.dentistry.appointment.common.NotFoundException;
import ru.itmo.dentistry.appointment.notification.AfterCommitNotificationPublisher;

@Service
public class AppointmentService {

    private static final String PATIENT = "PATIENT";
    private static final String DOCTOR = "DOCTOR";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentProcedureRepository appointmentProcedureRepository;
    private final CatalogGateway catalog;
    private final AfterCommitNotificationPublisher notifications;
    private final AppointmentCursorCodec cursorCodec;
    private final EntityManager entityManager;
    private final ZoneId clinicZone;
    private final int slotMinutes;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentProcedureRepository appointmentProcedureRepository,
            CatalogGateway catalog,
            AfterCommitNotificationPublisher notifications,
            AppointmentCursorCodec cursorCodec,
            EntityManager entityManager,
            @Value("#{T(java.time.ZoneId).of('${dentistry.appointment.zone-id}')}") ZoneId clinicZone,
            @Value("${dentistry.appointment.slot-minutes}") int slotMinutes) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentProcedureRepository = appointmentProcedureRepository;
        this.catalog = catalog;
        this.notifications = notifications;
        this.cursorCodec = cursorCodec;
        this.entityManager = entityManager;
        this.clinicZone = clinicZone;
        this.slotMinutes = slotMinutes;
    }

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        if (request.patientId().equals(request.doctorId())) {
            throw new BusinessValidationException("Patient and doctor must be different users");
        }
        var patient = catalog.user(request.patientId());
        var doctor = catalog.user(request.doctorId());
        lockParticipants(patient.id(), doctor.id());
        validateParticipants(patient, doctor);
        validateInterval(doctor, request.startsAt(), request.endsAt(), null, patient.id());

        var appointment = new Appointment();
        appointment.setPatientId(patient.id());
        appointment.setPatientName(patient.fullName());
        appointment.setDoctorId(doctor.id());
        appointment.setDoctorName(doctor.fullName());
        appointment.setStartsAt(request.startsAt());
        appointment.setEndsAt(request.endsAt());
        appointment.setComment(trimToNull(request.comment()));
        appointment = appointmentRepository.saveAndFlush(appointment);
        notifications.created(appointment.getId());
        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(UUID id) {
        return toResponse(requireDetailed(id));
    }

    @Transactional(readOnly = true)
    public AppointmentScrollResponse findAll(String cursor, int size) {
        var pageable = PageRequest.of(0, size);
        var slice = cursor == null || cursor.isBlank()
                ? appointmentRepository.findByOrderByCreatedAtDescIdDesc(pageable)
                : findAfterCursor(cursor, pageable);
        var items = slice.getContent().stream().map(this::toResponse).toList();
        String nextCursor = slice.hasNext() && !slice.getContent().isEmpty()
                ? cursorCodec.encode(new AppointmentCursor(
                        slice.getContent().getLast().getCreatedAt(),
                        slice.getContent().getLast().getId()))
                : null;
        return new AppointmentScrollResponse(items, nextCursor, slice.hasNext());
    }

    @Transactional
    public AppointmentResponse reschedule(UUID id, RescheduleAppointmentRequest request) {
        var appointment = lockAppointment(id);
        var doctor = catalog.user(appointment.getDoctorId());
        var patient = catalog.user(appointment.getPatientId());
        lockParticipants(patient.id(), doctor.id());
        requireScheduled(appointment);
        validateParticipants(patient, doctor);
        validateInterval(doctor, request.startsAt(), request.endsAt(), appointment.getId(), patient.id());
        appointment.setStartsAt(request.startsAt());
        appointment.setEndsAt(request.endsAt());
        appointment.setPatientName(patient.fullName());
        appointment.setDoctorName(doctor.fullName());
        appointmentRepository.flush();
        notifications.rescheduled(id);
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse complete(UUID id) {
        var appointment = lockAppointment(id);
        requireScheduled(appointment);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return toResponse(appointment);
    }

    @Transactional
    public void cancel(UUID id) {
        var appointment = lockAppointment(id);
        requireScheduled(appointment);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        notifications.cancelled(id);
    }

    @Transactional
    public AppointmentResponse addProcedure(UUID appointmentId, AppointmentProcedureRequest request) {
        var appointment = lockAppointment(appointmentId);
        requireScheduled(appointment);
        var procedure = catalog.procedure(request.procedureId());
        var doctor = catalog.user(appointment.getDoctorId());
        if (!procedure.active()) {
            throw new BusinessValidationException("Inactive procedure cannot be added to an appointment");
        }
        if (doctor.procedureIds() == null || !doctor.procedureIds().contains(procedure.id())) {
            throw new BusinessValidationException("The doctor does not perform this procedure");
        }
        var item = new AppointmentProcedure();
        item.setAppointment(appointment);
        item.setProcedureId(procedure.id());
        item.setProcedureName(procedure.name());
        item.setPrice(procedure.basePrice());
        item.setQuantity(request.quantity());
        item.setToothNumber(request.toothNumber());
        appointmentProcedureRepository.save(item);
        appointment.getProcedures().add(item);
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateProcedure(
            UUID appointmentId, UUID itemId, UpdateAppointmentProcedureRequest request) {
        var appointment = lockAppointment(appointmentId);
        requireScheduled(appointment);
        var item = appointmentProcedureRepository.findByIdAndAppointmentId(itemId, appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment procedure not found: " + itemId));
        item.setQuantity(request.quantity());
        item.setToothNumber(request.toothNumber());
        return toResponse(appointment);
    }

    @Transactional
    public void removeProcedure(UUID appointmentId, UUID itemId) {
        var appointment = lockAppointment(appointmentId);
        requireScheduled(appointment);
        var item = appointmentProcedureRepository.findByIdAndAppointmentId(itemId, appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment procedure not found: " + itemId));
        appointment.getProcedures().removeIf(current -> current.getId().equals(itemId));
        appointmentProcedureRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> availability(UUID doctorId, LocalDate date) {
        var doctor = catalog.user(doctorId);
        if (!DOCTOR.equals(doctor.role())) {
            throw new BusinessValidationException("User is not a doctor");
        }
        if (!doctor.active()) {
            throw new BusinessValidationException("Doctor is inactive");
        }
        var start = date.atTime(doctor.workDayStart()).atZone(clinicZone).toInstant();
        var end = date.atTime(doctor.workDayEnd()).atZone(clinicZone).toInstant();
        var appointments = appointmentRepository
                .findByDoctorIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAt(
                        doctorId, AppointmentStatus.SCHEDULED, end, start);
        var result = new java.util.ArrayList<AvailabilityResponse>();
        for (var current = start; !current.plusSeconds(slotMinutes * 60L).isAfter(end);
                current = current.plusSeconds(slotMinutes * 60L)) {
            var slotEnd = current.plusSeconds(slotMinutes * 60L);
            var slotStart = current;
            boolean occupied = appointments.stream()
                    .anyMatch(item -> item.getStartsAt().isBefore(slotEnd) && item.getEndsAt().isAfter(slotStart));
            if (!occupied) {
                result.add(new AvailabilityResponse(slotStart, slotEnd));
            }
        }
        return List.copyOf(result);
    }

    private void lockParticipants(UUID first, UUID second) {
        long left = lockKey(first);
        long right = lockKey(second);
        if (Long.compareUnsigned(left, right) <= 0) {
            advisoryLock(left);
            if (left != right) {
                advisoryLock(right);
            }
        } else {
            advisoryLock(right);
            advisoryLock(left);
        }
    }

    private long lockKey(UUID id) {
        return id.getMostSignificantBits() ^ id.getLeastSignificantBits();
    }

    private void advisoryLock(long key) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                .setParameter("key", key)
                .getSingleResult();
    }

    private Appointment requireDetailed(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
    }

    private Appointment lockAppointment(UUID id) {
        return appointmentRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
    }

    private void validateParticipants(CatalogUserView patient, CatalogUserView doctor) {
        if (!PATIENT.equals(patient.role())) {
            throw new BusinessValidationException("Patient user must have PATIENT role");
        }
        if (!DOCTOR.equals(doctor.role())) {
            throw new BusinessValidationException("Doctor user must have DOCTOR role");
        }
        if (!patient.active() || !doctor.active()) {
            throw new BusinessValidationException("Patient and doctor must be active");
        }
    }

    private void validateInterval(
            CatalogUserView doctor, Instant startsAt, Instant endsAt, UUID excludeId, UUID patientId) {
        if (!startsAt.isBefore(endsAt)) {
            throw new BusinessValidationException("Appointment start must be before its end");
        }
        var localStart = startsAt.atZone(clinicZone);
        var localEnd = endsAt.atZone(clinicZone);
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())
                || localStart.toLocalTime().isBefore(doctor.workDayStart())
                || localEnd.toLocalTime().isAfter(doctor.workDayEnd())) {
            throw new BusinessValidationException("Appointment must fit within the doctor's working hours");
        }
        if (appointmentRepository.hasDoctorOverlap(doctor.id(), startsAt, endsAt, excludeId)) {
            throw new ConflictException("Doctor already has an appointment during this time");
        }
        if (appointmentRepository.hasPatientOverlap(patientId, startsAt, endsAt, excludeId)) {
            throw new ConflictException("Patient already has an appointment during this time");
        }
    }

    private void requireScheduled(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Only a scheduled appointment can be changed");
        }
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        var procedures = appointment.getProcedures().stream().map(this::toProcedureResponse).toList();
        var total = procedures.stream()
                .map(AppointmentProcedureResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getPatientName(),
                appointment.getDoctorId(),
                appointment.getDoctorName(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                appointment.getStatus(),
                appointment.getComment(),
                procedures,
                total,
                appointment.getCreatedAt(),
                appointment.getUpdatedAt());
    }

    private AppointmentProcedureResponse toProcedureResponse(AppointmentProcedure item) {
        var total = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new AppointmentProcedureResponse(
                item.getId(),
                item.getProcedureId(),
                item.getProcedureName(),
                item.getPrice(),
                item.getQuantity(),
                item.getToothNumber(),
                total);
    }

    private org.springframework.data.domain.Slice<Appointment> findAfterCursor(
            String cursor, org.springframework.data.domain.Pageable pageable) {
        var decoded = cursorCodec.decode(cursor);
        return appointmentRepository.findAfterCursor(decoded.createdAt(), decoded.id(), pageable);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
