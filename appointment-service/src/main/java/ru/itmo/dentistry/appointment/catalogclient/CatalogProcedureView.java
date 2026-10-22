package ru.itmo.dentistry.appointment.catalogclient;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogProcedureView(
        UUID id,
        String name,
        BigDecimal basePrice,
        boolean active) {
}
