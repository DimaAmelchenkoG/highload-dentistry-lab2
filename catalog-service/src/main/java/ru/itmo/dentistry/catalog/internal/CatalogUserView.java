package ru.itmo.dentistry.catalog.internal;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record CatalogUserView(
        UUID id,
        String fullName,
        String role,
        boolean active,
        LocalTime workDayStart,
        LocalTime workDayEnd,
        Set<UUID> procedureIds) {
}
