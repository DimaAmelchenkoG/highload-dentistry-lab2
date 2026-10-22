package ru.itmo.dentistry.catalog.procedure;

import java.math.BigDecimal;
import java.util.UUID;

public record DentalProcedureResponse(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        int durationMinutes,
        boolean active) {
}
