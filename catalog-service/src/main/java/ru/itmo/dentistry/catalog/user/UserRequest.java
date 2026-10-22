package ru.itmo.dentistry.catalog.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.Set;

public record UserRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9() -]{7,32}$") String phone,
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull UserRole role,
        Set<DoctorSpecialization> specializations,
        LocalTime workDayStart,
        LocalTime workDayEnd) {
}
