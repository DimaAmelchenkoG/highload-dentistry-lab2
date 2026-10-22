package ru.itmo.dentistry.appointment.notification;

import java.util.UUID;

public interface NotificationService {

    void appointmentCreated(UUID appointmentId);

    void appointmentRescheduled(UUID appointmentId);

    void appointmentCancelled(UUID appointmentId);
}
