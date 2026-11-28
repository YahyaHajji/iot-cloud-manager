package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.dto.MeasurementInput;
import com.iotcloudmanager.backend.dto.TelemetryMessage;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.entity.Measurement;
import com.iotcloudmanager.backend.entity.MeasurementType;
import com.iotcloudmanager.backend.entity.ProcessedMessage;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import com.iotcloudmanager.backend.repository.MeasurementRepository;
import com.iotcloudmanager.backend.repository.MeasurementTypeRepository;
import com.iotcloudmanager.backend.repository.ProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    private static final String DEVICE_ID = "sensor-room-01";
    private static final String MESSAGE_ID = "3f1c9a2e-8b7d-4c1a-9e2f-1a2b3c4d5e6f";

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MeasurementRepository measurementRepository;

    @Mock
    private MeasurementTypeRepository measurementTypeRepository;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private TelemetryService telemetryService;

    private Device device;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setDeviceId(DEVICE_ID);
        device.setName("Capteur salle 1");
        device.setEnabled(true);
        device.setStatus(DeviceStatus.UNKNOWN);

        lenient().when(deviceRepository.findByDeviceId(DEVICE_ID))
                .thenReturn(Optional.of(device));
        lenient().when(processedMessageRepository.existsByMessageId(MESSAGE_ID))
                .thenReturn(false);
        lenient().when(measurementTypeRepository
                        .findByTypeAndEnabledTrue("temperature"))
                .thenReturn(Optional.of(
                        type("temperature", "°C", -40.0, 85.0)));
        lenient().when(measurementTypeRepository
                        .findByTypeAndEnabledTrue("humidity"))
                .thenReturn(Optional.of(
                        type("humidity", "%", 0.0, 100.0)));
    }

    private static MeasurementType type(
            String name, String unit, Double min, Double max) {
        MeasurementType type = new MeasurementType();
        type.setType(name);
        type.setUnit(unit);
        type.setMinValue(min);
        type.setMaxValue(max);
        type.setEnabled(true);
        return type;
    }

    private static MeasurementInput input(
            String type, Double value, String unit) {
        MeasurementInput input = new MeasurementInput();
        input.setType(type);
        input.setValue(value);
        input.setUnit(unit);
        return input;
    }

    private static TelemetryMessage message(MeasurementInput... inputs) {
        TelemetryMessage message = new TelemetryMessage();
        message.setSchemaVersion(1);
        message.setDeviceId(DEVICE_ID);
        message.setMessageId(MESSAGE_ID);
        message.setTimestamp(Instant.parse("2026-11-28T10:15:30Z"));
        message.setMeasurements(new ArrayList<>(List.of(inputs)));
        return message;
    }

    @Test
    void shouldSaveEveryMeasurementOfAValidMessage() {
        TelemetryMessage message = message(
                input("temperature", 24.5, "°C"),
                input("humidity", 52.0, "%"));

        String result = telemetryService.processTelemetry(DEVICE_ID, message);

        assertEquals("Message traité avec succès", result);

        ArgumentCaptor<Measurement> captor =
                ArgumentCaptor.forClass(Measurement.class);
        verify(measurementRepository, org.mockito.Mockito.times(2))
                .save(captor.capture());

        List<Measurement> saved = captor.getAllValues();
        assertEquals("temperature", saved.get(0).getType());
        assertEquals(24.5, saved.get(0).getValue());
        assertEquals("humidity", saved.get(1).getType());
        assertEquals(MESSAGE_ID, saved.get(0).getMessageId());
        assertEquals(device, saved.get(0).getDevice());
    }

    @Test
    void shouldMarkTheDeviceOnlineAndRecordTheMessage() {
        telemetryService.processTelemetry(DEVICE_ID,
                message(input("temperature", 24.5, "°C")));

        assertEquals(DeviceStatus.ONLINE, device.getStatus());
        assertTrue(device.getLastSeenAt() != null);
        verify(deviceRepository).save(device);

        ArgumentCaptor<ProcessedMessage> captor =
                ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(processedMessageRepository).save(captor.capture());
        assertEquals(MESSAGE_ID, captor.getValue().getMessageId());
        assertEquals(DEVICE_ID, captor.getValue().getDeviceId());
    }

    @Test
    void shouldCheckEachMeasurementAgainstTheAlertThresholds() {
        telemetryService.processTelemetry(DEVICE_ID, message(
                input("temperature", 31.0, "°C"),
                input("humidity", 52.0, "%")));

        verify(alertService).checkMeasurement(
                device, "temperature", 31.0, "°C");
        verify(alertService).checkMeasurement(
                device, "humidity", 52.0, "%");
    }

    @Test
    void shouldIgnoreADuplicateMessageWithoutSavingAnything() {
        when(processedMessageRepository.existsByMessageId(MESSAGE_ID))
                .thenReturn(true);

        String result = telemetryService.processTelemetry(DEVICE_ID,
                message(input("temperature", 24.5, "°C")));

        assertEquals("Message déjà traité", result);
        verify(measurementRepository, never()).save(any());
        verify(alertService, never())
                .checkMeasurement(any(), any(), any(), any());
        verify(processedMessageRepository, never()).save(any());
    }

    @Test
    void shouldRejectAnUnknownDevice() {
        when(deviceRepository.findByDeviceId(DEVICE_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID,
                        message(input("temperature", 24.5, "°C"))));

        assertEquals("Appareil non enregistré", error.getMessage());
        verify(measurementRepository, never()).save(any());
    }

    @Test
    void shouldRejectADisabledDevice() {
        device.setEnabled(false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID,
                        message(input("temperature", 24.5, "°C"))));

        assertEquals("Appareil désactivé", error.getMessage());
        verify(measurementRepository, never()).save(any());
    }

    @Test
    void shouldRejectAValueAboveTheCatalogueLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID,
                        message(input("temperature", 120.0, "°C"))));

        verify(measurementRepository, never()).save(any());
        verify(processedMessageRepository, never()).save(any());
    }

    @Test
    void shouldRejectAValueBelowTheCatalogueLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID,
                        message(input("temperature", -50.0, "°C"))));

        verify(measurementRepository, never()).save(any());
    }

    @Test
    void shouldRejectAWrongUnit() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID,
                        message(input("temperature", 24.5, "°F"))));

        assertEquals("Unité incorrecte pour temperature", error.getMessage());
    }

    @Test
    void shouldRejectAnUnknownMeasurementType() {
        when(measurementTypeRepository.findByTypeAndEnabledTrue("radiation"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID,
                        message(input("radiation", 1.0, "Sv"))));

        verify(measurementRepository, never()).save(any());
    }

    @Test
    void shouldSaveNothingWhenOneMeasurementOfTheMessageIsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID, message(
                        input("temperature", 24.5, "°C"),
                        input("humidity", 150.0, "%"))));

        verify(measurementRepository, never()).save(any());
    }

    @Test
    void shouldRejectANonFiniteValue() {
        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID,
                        message(input("temperature", Double.NaN, "°C"))));
    }

    @Test
    void shouldRejectADeviceIdThatDiffersFromTheTopic() {
        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry("sensor-room-02",
                        message(input("temperature", 24.5, "°C"))));

        verify(deviceRepository, never()).findByDeviceId(eq("sensor-room-02"));
    }

    @Test
    void shouldRejectAnUnsupportedSchemaVersion() {
        TelemetryMessage message =
                message(input("temperature", 24.5, "°C"));
        message.setSchemaVersion(2);

        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID, message));
    }

    @Test
    void shouldRejectAMessageWithoutMeasurements() {
        TelemetryMessage message = message();

        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID, message));
    }

    @Test
    void shouldRejectAMessageWithMoreThanTenMeasurements() {
        MeasurementInput[] inputs = new MeasurementInput[11];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = input("temperature", 20.0, "°C");
        }

        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(
                        DEVICE_ID, message(inputs)));
    }

    @Test
    void shouldRejectAMessageWithoutMessageId() {
        TelemetryMessage message =
                message(input("temperature", 24.5, "°C"));
        message.setMessageId(" ");

        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID, message));
    }

    @Test
    void shouldRejectANullMessage() {
        assertThrows(IllegalArgumentException.class,
                () -> telemetryService.processTelemetry(DEVICE_ID, null));
    }
}
