package com.iotcloudmanager.backend.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AlertEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(AlertEmailService.class);
    private static final Duration NOTIFICATION_INTERVAL = Duration.ofMinutes(10);

    private final JavaMailSender mailSender;
    private final TaskExecutor executor;
    private final Clock clock;
    private final String smtpHost;
    private final String senderAddress;
    private final String recipientAddress;
    private final ConcurrentMap<String, Instant> lastScheduledByDeviceAndType =
            new ConcurrentHashMap<>();

    public AlertEmailService(
            JavaMailSender mailSender,
            @Qualifier("alertMailExecutor") TaskExecutor executor,
            Clock alertClock,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${spring.mail.username:}") String senderAddress,
            @Value("${app.alert.email-to:}") String recipientAddress
    ) {
        this.mailSender = mailSender;
        this.executor = executor;
        this.clock = alertClock;
        this.smtpHost = smtpHost;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
    }

    @PostConstruct
    public void logConfigurationStatus() {
        if (smtpHost.isBlank() || recipientAddress.isBlank()) {
            log.warn("E-mail d'alerte désactivé : SMTP_HOST ou ALERT_EMAIL_TO absent.");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendForAlert(AlertCreatedEvent alert) {
        if (smtpHost.isBlank() || recipientAddress.isBlank()) {
            return;
        }

        String key = alert.deviceId() + ":" + alert.type();
        Instant scheduledAt = clock.instant();
        synchronized (lastScheduledByDeviceAndType) {
            Instant previous = lastScheduledByDeviceAndType.get(key);
            if (previous != null
                    && scheduledAt.isBefore(previous.plus(NOTIFICATION_INTERVAL))) {
                return;
            }
            lastScheduledByDeviceAndType.put(key, scheduledAt);
        }

        try {
            executor.execute(() -> sendEmail(alert));
        } catch (TaskRejectedException e) {
            lastScheduledByDeviceAndType.remove(key, scheduledAt);
            log.error(
                    "E-mail d'alerte non mis en file pour {} / {}.",
                    alert.deviceId(),
                    alert.type(),
                    e
            );
        }
    }

    private void sendEmail(AlertCreatedEvent alert) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientAddress);
        if (!senderAddress.isBlank()) {
            message.setFrom(senderAddress);
        }
        message.setSubject(
                "IoT Cloud Manager - alerte " + alert.type()
                        + " (" + alert.deviceName() + ")"
        );
        message.setText(
                "Une alerte a été déclenchée.\n\n"
                        + "Capteur : " + alert.deviceName()
                        + " (" + alert.deviceId() + ")\n"
                        + "Mesure : " + alert.type() + "\n"
                        + "Valeur : " + alert.value() + " " + alert.unit() + "\n"
                        + "Date : " + alert.triggeredAt() + "\n"
        );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error(
                    "Échec de l'envoi de l'e-mail d'alerte pour {} / {}.",
                    alert.deviceId(),
                    alert.type(),
                    e
            );
        }
    }
}
