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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Service
public class TelemetryService {

    public static final String RESULT_PROCESSED = "Message traité avec succès";
    public static final String RESULT_DUPLICATE = "Message déjà traité";

    private final DeviceRepository deviceRepository;
    private final MeasurementRepository measurementRepository;
    private final MeasurementTypeRepository measurementTypeRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final AlertService alertService;

    public TelemetryService(
            DeviceRepository deviceRepository,
            MeasurementRepository measurementRepository,
            MeasurementTypeRepository measurementTypeRepository,
            ProcessedMessageRepository processedMessageRepository,
            AlertService alertService) {
        this.deviceRepository = deviceRepository;
        this.measurementRepository = measurementRepository;
        this.measurementTypeRepository = measurementTypeRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.alertService = alertService;
    }

    @Transactional
    public String processTelemetry(String topicDeviceId, TelemetryMessage message) {

        if (message == null) {
            throw new IllegalArgumentException("Message de télémétrie absent");
        }

        if (message.getSchemaVersion() == null
                || message.getSchemaVersion() != 1) {
            throw new IllegalArgumentException("Version du schéma invalide");
        }

        if (topicDeviceId == null
                || message.getDeviceId() == null
                || !topicDeviceId.equals(message.getDeviceId())) {
            throw new IllegalArgumentException(
                    "Le deviceId ne correspond pas au topic MQTT");
        }

        if (message.getMessageId() == null || message.getMessageId().isBlank()) {
            throw new IllegalArgumentException("messageId invalide");
        }

        if (message.getTimestamp() == null) {
            throw new IllegalArgumentException("Timestamp manquant");
        }

        if (message.getMeasurements() == null
                || message.getMeasurements().isEmpty()
                || message.getMeasurements().size() > 10) {
            throw new IllegalArgumentException(
                    "Le message doit contenir entre 1 et 10 mesures");
        }

        Device device = deviceRepository.findByDeviceId(topicDeviceId)
                .orElseThrow(() ->
                        new MessageRejectedException(
                                "unknown_device", "Appareil non enregistré"));

        if (!device.isEnabled()) {
            throw new MessageRejectedException(
                    "device_disabled", "Appareil désactivé");
        }

        if (processedMessageRepository.existsByMessageId(message.getMessageId())) {
            return RESULT_DUPLICATE;
        }


        for (MeasurementInput input : message.getMeasurements()) {

            if (input == null
                    || input.getType() == null
                    || input.getType().isBlank()
                    || input.getUnit() == null
                    || input.getUnit().isBlank()) {
                throw new IllegalArgumentException("Champs de mesure invalides");
            }

            MeasurementType type = measurementTypeRepository
                    .findByTypeAndEnabledTrue(input.getType())
                    .orElseThrow(() ->
                            new MessageRejectedException(
                                    "unknown_measurement_type",
                                    "Type de mesure inconnu ou désactivé : "
                                            + input.getType()));

            if (!type.getUnit().equals(input.getUnit())) {
                throw new MessageRejectedException(
                        "invalid_unit",
                        "Unité incorrecte pour " + input.getType());
            }

            Double value = input.getValue();

            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Valeur de mesure invalide");
            }

            if (type.getMinValue() != null && value < type.getMinValue()) {
                throw new MessageRejectedException(
                        "value_out_of_range",
                        "Valeur inférieure à la limite autorisée pour "
                                + input.getType());
            }

            if (type.getMaxValue() != null && value > type.getMaxValue()) {
                throw new MessageRejectedException(
                        "value_out_of_range",
                        "Valeur supérieure à la limite autorisée pour "
                                + input.getType());
            }

            
        }

        LocalDateTime measuredAt = LocalDateTime.ofInstant(
                message.getTimestamp(), ZoneOffset.UTC);

        LocalDateTime receivedAt = LocalDateTime.ofInstant(
                Instant.now(), ZoneOffset.UTC);

        for (MeasurementInput input : message.getMeasurements()) {

            Measurement measurement = new Measurement();
            measurement.setDevice(device);
            measurement.setMessageId(message.getMessageId());
            measurement.setType(input.getType());
            measurement.setValue(input.getValue());
            measurement.setUnit(input.getUnit());
            measurement.setMeasuredAt(measuredAt);
            measurement.setReceivedAt(receivedAt);

            measurementRepository.save(measurement);

            alertService.checkMeasurement(
                    device,
                    input.getType(),
                    input.getValue(),
                    input.getUnit());
        }

        ProcessedMessage processedMessage = new ProcessedMessage();
        processedMessage.setMessageId(message.getMessageId());
        processedMessage.setDeviceId(message.getDeviceId());
        processedMessage.setProcessedAt(receivedAt);

        processedMessageRepository.save(processedMessage);

        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(Instant.now());

        deviceRepository.save(device);

        return RESULT_PROCESSED;
    }
}
