
package com.iotcloudmanager.backend;

import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import com.iotcloudmanager.backend.service.DeviceOfflineScheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceOfflineSchedulerTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceOfflineScheduler scheduler;

    private Device device;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setDeviceId("sensor-001");
        device.setName("Capteur température");
        device.setEnabled(true);
        device.setIntervalSec(5);
        device.setStatus(DeviceStatus.ONLINE);
    }

    @Test
    void shouldSetDeviceOfflineWhenTimeoutExceeded() {
        device.setLastSeenAt(Instant.now().minusSeconds(20));

        when(deviceRepository.findByEnabledTrue())
                .thenReturn(List.of(device));

        scheduler.detectOfflineDevices();

        assertEquals(DeviceStatus.OFFLINE, device.getStatus());
        verify(deviceRepository).save(device);
    }

    @Test
    void shouldKeepDeviceOnlineWhenRecentlySeen() {
        device.setLastSeenAt(Instant.now());

        when(deviceRepository.findByEnabledTrue())
                .thenReturn(List.of(device));

        scheduler.detectOfflineDevices();

        assertEquals(DeviceStatus.ONLINE, device.getStatus());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void shouldIgnoreDeviceWithoutLastSeenAt() {
        device.setLastSeenAt(null);

        when(deviceRepository.findByEnabledTrue())
                .thenReturn(List.of(device));

        scheduler.detectOfflineDevices();

        assertEquals(DeviceStatus.ONLINE, device.getStatus());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void shouldNotSaveDeviceAlreadyOffline() {
        device.setStatus(DeviceStatus.OFFLINE);
        device.setLastSeenAt(Instant.now().minusSeconds(30));

        when(deviceRepository.findByEnabledTrue())
                .thenReturn(List.of(device));

        scheduler.detectOfflineDevices();

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void shouldHandleEmptyDeviceList() {
        when(deviceRepository.findByEnabledTrue())
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> scheduler.detectOfflineDevices());

        verify(deviceRepository, never()).save(any(Device.class));
    }
}
