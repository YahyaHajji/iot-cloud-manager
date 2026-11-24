
package com.iotcloudmanager.backend;

import com.iotcloudmanager.backend.dto.DeviceRequest;
import com.iotcloudmanager.backend.dto.DeviceResponse;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import com.iotcloudmanager.backend.repository.MeasurementRepository;
import com.iotcloudmanager.backend.service.DeviceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MeasurementRepository measurementRepository;

    @InjectMocks
    private DeviceService deviceService;

    private DeviceRequest request;
    private Device device;

    @BeforeEach
    void setUp() {
        request = new DeviceRequest();
        request.setDeviceId("sensor-001");
        request.setName("Capteur température");
        request.setIntervalSec(60);
        request.setLocation("Salle serveur");
        request.setDescription("Capteur de température");

        device = new Device();
        device.setDeviceId("sensor-001");
        device.setName("Capteur température");
        device.setIntervalSec(60);
        device.setEnabled(true);
        device.setStatus(DeviceStatus.UNKNOWN);
        device.setLocation("Salle serveur");
        device.setDescription("Capteur de température");
    }

    @Test
    void shouldCreateDevice() {
        when(deviceRepository.existsByDeviceId("sensor-001"))
                .thenReturn(false);

        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.createDevice(request);

        assertEquals("sensor-001", result.getDeviceId());
        assertEquals("Capteur température", result.getName());
        assertEquals(60, result.getIntervalSec());
        assertTrue(result.isEnabled());
        assertEquals(DeviceStatus.UNKNOWN, result.getStatus());
        assertEquals("Salle serveur", result.getLocation());
        assertEquals("Capteur de température", result.getDescription());

        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void shouldRejectDuplicateDeviceId() {
        when(deviceRepository.existsByDeviceId("sensor-001"))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> deviceService.createDevice(request));

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void shouldGetAllDevices() {
        when(deviceRepository.findAll())
                .thenReturn(Collections.singletonList(device));

        when(measurementRepository
                .findByDeviceDeviceIdOrderByMeasuredAtDesc("sensor-001"))
                .thenReturn(Collections.emptyList());

        var result = deviceService.getAllDevices();

        assertEquals(1, result.size());

        DeviceResponse response = result.get(0);

        assertEquals("sensor-001", response.getDeviceId());
        assertEquals("Salle serveur", response.getLocation());
        assertEquals("Capteur de température", response.getDescription());
        assertNotNull(response.getLastMeasurements());
        assertTrue(response.getLastMeasurements().isEmpty());

        verify(deviceRepository).findAll();
        verify(measurementRepository)
                .findByDeviceDeviceIdOrderByMeasuredAtDesc("sensor-001");
    }

    @Test
    void shouldGetDeviceById() {
        when(deviceRepository.findById(1L))
                .thenReturn(Optional.of(device));

        Device result = deviceService.getDeviceById(1L);

        assertEquals("sensor-001", result.getDeviceId());
    }

    @Test
    void shouldThrowWhenDeviceNotFound() {
        when(deviceRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> deviceService.getDeviceById(99L));
    }

    @Test
    void shouldUpdateDevice() {
        request.setName("Capteur modifié");
        request.setLocation("Nouveau local");
        request.setDescription("Description modifiée");

        when(deviceRepository.findById(1L))
                .thenReturn(Optional.of(device));

        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.updateDevice(1L, request);

        assertEquals("Capteur modifié", result.getName());
        assertEquals("Nouveau local", result.getLocation());
        assertEquals("Description modifiée", result.getDescription());

        verify(deviceRepository).save(device);
    }

    @Test
    void shouldDisableDevice() {
        when(deviceRepository.findById(1L))
                .thenReturn(Optional.of(device));

        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.changeEnabled(1L, false);

        assertFalse(result.isEnabled());
        assertEquals(DeviceStatus.OFFLINE, result.getStatus());
    }

    @Test
    void shouldEnableDevice() {
        device.setStatus(DeviceStatus.OFFLINE);

        when(deviceRepository.findById(1L))
                .thenReturn(Optional.of(device));

        when(deviceRepository.save(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.changeEnabled(1L, true);

        assertTrue(result.isEnabled());
        assertEquals(DeviceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldDeleteDevice() {
        when(deviceRepository.findById(1L))
                .thenReturn(Optional.of(device));

        deviceService.deleteDevice(1L);

        verify(deviceRepository).delete(device);
    }
}
