package ru.itmo.dentistry.appointment;

import java.time.Instant;

public record AvailabilityResponse(Instant startsAt, Instant endsAt) {
}
