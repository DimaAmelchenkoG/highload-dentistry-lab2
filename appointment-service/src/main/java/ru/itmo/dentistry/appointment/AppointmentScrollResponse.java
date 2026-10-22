package ru.itmo.dentistry.appointment;

import java.util.List;

public record AppointmentScrollResponse(
        List<AppointmentResponse> items,
        String nextCursor,
        boolean hasNext) {
}
