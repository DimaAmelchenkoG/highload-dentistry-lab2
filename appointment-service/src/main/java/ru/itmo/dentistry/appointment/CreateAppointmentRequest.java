package ru.itmo.dentistry.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID doctorId,
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt,
        @Size(max = 1000) String comment) {
}
