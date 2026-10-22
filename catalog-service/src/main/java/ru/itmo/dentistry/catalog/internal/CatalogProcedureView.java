package ru.itmo.dentistry.catalog.internal;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogProcedureView(
        UUID id,
        String name,
        BigDecimal basePrice,
        boolean active) {
}
