package ru.itmo.dentistry.appointment;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.itmo.dentistry.appointment.common.BusinessValidationException;

@Component
class AppointmentCursorCodec {

    String encode(AppointmentCursor cursor) {
        var value = cursor.createdAt() + "|" + cursor.id();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    AppointmentCursor decode(String value) {
        try {
            var decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            var parts = decoded.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Cursor must have two parts");
            }
            return new AppointmentCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new BusinessValidationException("Invalid appointment cursor");
        }
    }
}
