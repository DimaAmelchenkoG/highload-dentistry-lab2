package ru.itmo.dentistry.appointment;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Appointment> findLockedById(UUID id);

    Slice<Appointment> findByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
            select a from Appointment a
            where a.createdAt < :createdAt
               or (a.createdAt = :createdAt and a.id < :id)
            order by a.createdAt desc, a.id desc
            """)
    Slice<Appointment> findAfterCursor(
            @Param("createdAt") Instant createdAt,
            @Param("id") UUID id,
            Pageable pageable);

    @Query("""
            select (count(a) > 0) from Appointment a
            where a.doctorId = :doctorId
              and a.status = ru.itmo.dentistry.appointment.AppointmentStatus.SCHEDULED
              and a.startsAt < :endsAt and a.endsAt > :startsAt
              and (:excludeId is null or a.id <> :excludeId)
            """)
    boolean hasDoctorOverlap(
            @Param("doctorId") UUID doctorId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt,
            @Param("excludeId") UUID excludeId);

    @Query("""
            select (count(a) > 0) from Appointment a
            where a.patientId = :patientId
              and a.status = ru.itmo.dentistry.appointment.AppointmentStatus.SCHEDULED
              and a.startsAt < :endsAt and a.endsAt > :startsAt
              and (:excludeId is null or a.id <> :excludeId)
            """)
    boolean hasPatientOverlap(
            @Param("patientId") UUID patientId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt,
            @Param("excludeId") UUID excludeId);

    List<Appointment> findByDoctorIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAt(
            UUID doctorId, AppointmentStatus status, Instant dayEnd, Instant dayStart);
}
