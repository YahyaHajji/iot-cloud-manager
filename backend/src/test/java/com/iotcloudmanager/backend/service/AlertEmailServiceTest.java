package com.iotcloudmanager.backend.service;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.core.task.TaskExecutor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlertEmailServiceTest {

    private GreenMail greenMail;
    private JavaMailSenderImpl mailSender;
    private MutableClock clock;
    private AlertEmailService alertEmailService;

    @BeforeEach
    void setUp() {
        greenMail = new GreenMail(
                new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP)
        );
        greenMail.start();

        mailSender = new JavaMailSenderImpl();
        mailSender.setHost("127.0.0.1");
        mailSender.setPort(greenMail.getSmtp().getPort());
        mailSender.setUsername("sender@example.test");
        mailSender.getJavaMailProperties().put("mail.smtp.auth", "false");
        mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", "false");

        clock = new MutableClock(Instant.parse("2026-11-28T10:00:00Z"));
        TaskExecutor directExecutor = Runnable::run;
        alertEmailService = new AlertEmailService(
                mailSender,
                directExecutor,
                clock,
                "127.0.0.1",
                "sender@example.test",
                "alerts@example.test"
        );
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    void sendsAlertThroughSimulatedSmtpAndThrottlesForTenMinutes()
            throws Exception {
        AlertCreatedEvent event = alertEvent("sensor-room-01", "temperature");

        alertEmailService.sendForAlert(event);
        assertEquals(1, receivedMessages().length);
        MimeMessage firstMessage = receivedMessages()[0];
        assertTrue(firstMessage.getSubject().contains("temperature"));
        assertTrue(firstMessage.getContent().toString().contains("sensor-room-01"));

        clock.advance(Duration.ofMinutes(9));
        alertEmailService.sendForAlert(event);
        assertEquals(1, receivedMessages().length);

        clock.advance(Duration.ofMinutes(2));
        alertEmailService.sendForAlert(event);
        assertEquals(2, receivedMessages().length);
    }

    @Test
    void throttlesSeparatelyByDeviceAndMeasurementType() throws Exception {
        alertEmailService.sendForAlert(
                alertEvent("sensor-room-01", "temperature")
        );
        alertEmailService.sendForAlert(
                alertEvent("sensor-room-02", "temperature")
        );
        alertEmailService.sendForAlert(
                alertEvent("sensor-room-01", "humidity")
        );

        assertEquals(3, receivedMessages().length);
    }

    @Test
    void mailFailureDoesNotEscapeIntoAlertProcessing() {
        JavaMailSender failingSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("simulated SMTP failure"))
                .when(failingSender)
                .send(any(org.springframework.mail.SimpleMailMessage.class));
        AlertEmailService service = new AlertEmailService(
                failingSender,
                (TaskExecutor) Runnable::run,
                clock,
                "smtp.example.test",
                "sender@example.test",
                "alerts@example.test"
        );

        assertDoesNotThrow(() ->
                service.sendForAlert(
                        alertEvent("sensor-room-01", "temperature")
                )
        );
        verify(failingSender).send(any(
                org.springframework.mail.SimpleMailMessage.class
        ));
    }

    private MimeMessage[] receivedMessages() {
        return greenMail.getReceivedMessages();
    }

    private AlertCreatedEvent alertEvent(String deviceId, String type) {
        return new AlertCreatedEvent(
                deviceId,
                "Test sensor",
                type,
                31.0,
                "unit",
                LocalDateTime.of(2026, 11, 28, 10, 0)
        );
    }

    private static class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;

        private MutableClock(Instant initialInstant) {
            instant = new AtomicReference<>(initialInstant);
        }

        private void advance(Duration duration) {
            instant.updateAndGet(current -> current.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
