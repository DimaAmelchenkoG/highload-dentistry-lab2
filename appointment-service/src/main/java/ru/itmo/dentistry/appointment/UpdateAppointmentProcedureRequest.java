package ru.itmo.dentistry.appointment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateAppointmentProcedureRequest(
        @NotNull @Positive Integer quantity,
        @Min(1) @Max(32) Integer toothNumber) {
}
