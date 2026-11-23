
package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.dto.DeviceStatusMessage;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import com.iotcloudmanager.backend.repository.ProcessedMessageRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceStatusServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @InjectMocks
    private DeviceStatusService deviceStatusService;

    private Device device;
    private DeviceStatusMessage message;

    @BeforeEach
    void setUp() {

        device = new Device();

        device.setDeviceId("sensor-001");
        device.setName("Capteur température");
        device.setEnabled(true);
        device.setStatus(DeviceStatus.UNKNOWN);
        device.setIntervalSec(60);

        message = new DeviceStatusMessage();

        message.setSchemaVersion(1);
        message.setDeviceId("sensor-001");
        message.setMessageId("message-001");
        message.setStatus("ONLINE");
        message.setIntervalSec(30);
        message.setFirmware("1.0.0");
        message.setTimestamp(
                Instant.parse("2026-11-23T10:00:00Z"));
    }

    @Test
    void shouldSetDeviceOnline() {

        when(processedMessageRepository.existsByMessageId(
                "message-001")).thenReturn(false);

        when(deviceRepository.findByDeviceId("sensor-001"))
                .thenReturn(Optional.of(device));

        String result = deviceStatusService.processStatus(
                "sensor-001", message);

        assertEquals(
                "Statut mis à jour : ONLINE", result);

        assertEquals(DeviceStatus.ONLINE, device.getStatus());
        assertEquals(30, device.getIntervalSec());
        assertEquals("1.0.0", device.getFirmware());

        assertEquals(
                Instant.parse("2026-11-23T10:00:00Z"),
                device.getLastSeenAt());

        verify(deviceRepository).save(device);
        verify(processedMessageRepository).save(any());
    }

    @Test
    void shouldSetDeviceOffline() {

        message.setStatus("OFFLINE");

        when(processedMessageRepository.existsByMessageId(
                "message-001")).thenReturn(false);

        when(deviceRepository.findByDeviceId("sensor-001"))
                .thenReturn(Optional.of(device));

        String result = deviceStatusService.processStatus(
                "sensor-001", message);

        assertEquals(
                "Statut mis à jour : OFFLINE", result);

        assertEquals(DeviceStatus.OFFLINE, device.getStatus());

        verify(deviceRepository).save(device);
        verify(processedMessageRepository).save(any());
    }

    @Test
    void shouldIgnoreDuplicateMessage() {

        when(processedMessageRepository.existsByMessageId(
                "message-001")).thenReturn(true);

        String result = deviceStatusService.processStatus(
                "sensor-001", message);

        assertEquals(
                "Message de statut déjà traité", result);

        verify(deviceRepository, never()).save(any());
        verify(processedMessageRepository, never()).save(any());
    }

    @Test
    void shouldRejectNullMessage() {

        assertThrows(
                IllegalArgumentException.class,
                () -> deviceStatusService.processStatus(
                        "sensor-001", null));

        verifyNoInteractions(deviceRepository);
    }

    @Test
    void shouldRejectWrongDeviceId() {

        message.setDeviceId("sensor-002");

        assertThrows(
                IllegalArgumentException.class,
                () -> deviceStatusService.processStatus(
                        "sensor-001", message));

        verifyNoInteractions(deviceRepository);
    }

    @Test
    void shouldRejectUnknownDevice() {

        when(processedMessageRepository.existsByMessageId(
                "message-001")).thenReturn(false);

        when(deviceRepository.findByDeviceId("sensor-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> deviceStatusService.processStatus(
                        "sensor-001", message));

        verify(deviceRepository, never()).save(any());
    }

    @Test
    void shouldRejectDisabledDevice() {

        device.setEnabled(false);

        when(processedMessageRepository.existsByMessageId(
                "message-001")).thenReturn(false);

        when(deviceRepository.findByDeviceId("sensor-001"))
                .thenReturn(Optional.of(device));

        assertThrows(
                IllegalArgumentException.class,
                () -> deviceStatusService.processStatus(
                        "sensor-001", message));

        verify(deviceRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidInterval() {

        message.setIntervalSec(0);

        when(processedMessageRepository.existsByMessageId(
                "message-001")).thenReturn(false);

        when(deviceRepository.findByDeviceId("sensor-001"))
                .thenReturn(Optional.of(device));

        assertThrows(
                IllegalArgumentException.class,
                () -> deviceStatusService.processStatus(
                        "sensor-001", message));

        verify(deviceRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownStatus() {

        message.setStatus("UNKNOWN_STATUS");

        when(processedMessageRepository.existsByMessageId(
                "message-001")).thenReturn(false);

        when(deviceRepository.findByDeviceId("sensor-001"))
                .thenReturn(Optional.of(device));

        assertThrows(
                IllegalArgumentException.class,
                () -> deviceStatusService.processStatus(
                        "sensor-001", message));

        verify(deviceRepository, never()).save(any());
    }
}
