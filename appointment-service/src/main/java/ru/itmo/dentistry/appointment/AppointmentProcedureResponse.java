package ru.itmo.dentistry.appointment;

import java.math.BigDecimal;
import java.util.UUID;

public record AppointmentProcedureResponse(
        UUID id,
        UUID procedureId,
        String procedureName,
        BigDecimal price,
        int quantity,
        Integer toothNumber,
        BigDecimal total) {
}
