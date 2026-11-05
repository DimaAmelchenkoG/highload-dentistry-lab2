package ru.itmo.dentistry.appointment.notification;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {

    @Override
    public void appointmentCreated(UUID appointmentId) {
        log.info("Appointment {} created", appointmentId);
    }

    @Override
    public void appointmentRescheduled(UUID appointmentId) {
        log.info("Appointment {} rescheduled", appointmentId);
    }

    @Override
    public void appointmentCancelled(UUID appointmentId) {
        log.info("Appointment {} cancelled", appointmentId);
    }
}
