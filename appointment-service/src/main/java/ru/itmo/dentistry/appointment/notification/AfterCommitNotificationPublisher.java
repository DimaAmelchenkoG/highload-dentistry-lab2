package ru.itmo.dentistry.appointment.notification;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class AfterCommitNotificationPublisher {

    private final NotificationService notificationService;

    public void created(UUID appointmentId) {
        afterCommit(() -> notificationService.appointmentCreated(appointmentId));
    }

    public void rescheduled(UUID appointmentId) {
        afterCommit(() -> notificationService.appointmentRescheduled(appointmentId));
    }

    public void cancelled(UUID appointmentId) {
        afterCommit(() -> notificationService.appointmentCancelled(appointmentId));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
