package ru.itmo.dentistry.appointment;

import java.time.Instant;
import java.util.UUID;

record AppointmentCursor(Instant createdAt, UUID id) {
}
