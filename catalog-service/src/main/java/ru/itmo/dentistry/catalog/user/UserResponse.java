package ru.itmo.dentistry.catalog.user;

import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String phone,
        String email,
        UserRole role,
        boolean active,
        Set<DoctorSpecialization> specializations,
        LocalTime workDayStart,
        LocalTime workDayEnd,
        Set<UUID> procedureIds) {
}
