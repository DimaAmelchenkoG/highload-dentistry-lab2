package ru.itmo.dentistry.appointment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record AppointmentProcedureRequest(
        @NotNull UUID procedureId,
        @NotNull @Positive Integer quantity,
        @Min(1) @Max(32) Integer toothNumber) {
}
