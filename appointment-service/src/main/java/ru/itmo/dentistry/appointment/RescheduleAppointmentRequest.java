package ru.itmo.dentistry.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RescheduleAppointmentRequest(
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt) {
}
