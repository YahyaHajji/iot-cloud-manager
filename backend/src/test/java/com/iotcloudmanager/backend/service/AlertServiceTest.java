package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.entity.Alert;
import com.iotcloudmanager.backend.entity.AlertThreshold;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.repository.AlertRepository;
import com.iotcloudmanager.backend.repository.AlertThresholdRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final String DEVICE_ID = "sensor-room-01";

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertThresholdRepository thresholdRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AlertService alertService;

    private Device device;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setDeviceId(DEVICE_ID);
        device.setName("Capteur salle 1");
    }

    private void thresholdExists(Double min, Double max) {
        AlertThreshold threshold = new AlertThreshold();
        threshold.setDevice(device);
        threshold.setType("temperature");
        threshold.setMinThreshold(min);
        threshold.setMaxThreshold(max);
        threshold.setEnabled(true);

        when(thresholdRepository
                .findByDeviceDeviceIdAndTypeAndEnabledTrue(
                        DEVICE_ID, "temperature"))
                .thenReturn(Optional.of(threshold));
    }

    @Test
    void shouldCreateAnAlertWhenTheValueExceedsTheMaximum() {
        thresholdExists(10.0, 27.0);

        alertService.checkMeasurement(device, "temperature", 31.0, "°C");

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());

        Alert alert = captor.getValue();
        assertEquals(device, alert.getDevice());
        assertEquals("temperature", alert.getType());
        assertEquals(31.0, alert.getValue());
        assertEquals("°C", alert.getUnit());
        assertEquals("WARNING", alert.getSeverity());
        assertNotNull(alert.getTriggeredAt());
        verify(eventPublisher).publishEvent(any(AlertCreatedEvent.class));
    }

    @Test
    void shouldCreateAnAlertWhenTheValueIsBelowTheMinimum() {
        thresholdExists(10.0, 27.0);

        alertService.checkMeasurement(device, "temperature", 5.0, "°C");

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void shouldNotCreateAnAlertWhenTheValueIsInsideTheRange() {
        thresholdExists(10.0, 27.0);

        alertService.checkMeasurement(device, "temperature", 20.0, "°C");

        verify(alertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldNotCreateAnAlertExactlyOnTheLimits() {
        thresholdExists(10.0, 27.0);

        alertService.checkMeasurement(device, "temperature", 27.0, "°C");
        alertService.checkMeasurement(device, "temperature", 10.0, "°C");

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldNotCreateAnAlertWhenNoThresholdIsConfigured() {
        when(thresholdRepository
                .findByDeviceDeviceIdAndTypeAndEnabledTrue(
                        DEVICE_ID, "temperature"))
                .thenReturn(Optional.empty());

        alertService.checkMeasurement(device, "temperature", 99.0, "°C");

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreTheMinimumWhenOnlyAMaximumIsConfigured() {
        thresholdExists(null, 27.0);

        alertService.checkMeasurement(device, "temperature", -30.0, "°C");

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreTheMaximumWhenOnlyAMinimumIsConfigured() {
        thresholdExists(10.0, null);

        alertService.checkMeasurement(device, "temperature", 80.0, "°C");

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldReturnAllAlertsFromTheRepository() {
        Alert alert = new Alert();
        when(alertRepository.findAllByOrderByTriggeredAtDesc())
                .thenReturn(List.of(alert));

        assertEquals(List.of(alert), alertService.getAllAlerts());
    }

    @Test
    void shouldReturnTheAlertsOfADevice() {
        Alert alert = new Alert();
        when(alertRepository
                .findByDeviceDeviceIdOrderByTriggeredAtDesc(DEVICE_ID))
                .thenReturn(List.of(alert));

        assertEquals(List.of(alert), alertService.getDeviceAlerts(DEVICE_ID));
    }
}
