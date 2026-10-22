package ru.itmo.dentistry.appointment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID doctorId,
        String doctorName,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status,
        String comment,
        List<AppointmentProcedureResponse> procedures,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant updatedAt) {
}
