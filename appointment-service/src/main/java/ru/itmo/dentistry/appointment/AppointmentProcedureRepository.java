package ru.itmo.dentistry.appointment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentProcedureRepository extends JpaRepository<AppointmentProcedure, UUID> {

    Optional<AppointmentProcedure> findByIdAndAppointmentId(UUID id, UUID appointmentId);
}
