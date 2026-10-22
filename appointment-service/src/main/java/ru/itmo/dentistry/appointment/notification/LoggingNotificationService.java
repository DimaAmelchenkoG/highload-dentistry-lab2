package ru.itmo.dentistry.appointment.notification;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

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
