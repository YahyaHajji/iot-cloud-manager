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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TelemetryService {

    private final DeviceRepository deviceRepository;
    private final MeasurementRepository measurementRepository;
    private final MeasurementTypeRepository measurementTypeRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    public TelemetryService(
            DeviceRepository deviceRepository,
            MeasurementRepository measurementRepository,
            MeasurementTypeRepository measurementTypeRepository,
            ProcessedMessageRepository processedMessageRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.measurementRepository = measurementRepository;
        this.measurementTypeRepository = measurementTypeRepository;
        this.processedMessageRepository = processedMessageRepository;
    }

    @Transactional
    public String processTelemetry(String topicDeviceId, TelemetryMessage message) {

        // 1. Vérifier la version du schéma
        if (message.getSchemaVersion() == null
                || message.getSchemaVersion() != 1) {
            throw new IllegalArgumentException("Version du schéma invalide");
        }

        // 2. Vérifier que le deviceId correspond au topic MQTT
        if (!topicDeviceId.equals(message.getDeviceId())) {
            throw new IllegalArgumentException(
                    "Le deviceId ne correspond pas au topic MQTT"
            );
        }

        // 3. Vérifier que l'appareil existe
        Device device = deviceRepository.findByDeviceId(topicDeviceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Appareil non enregistré")
                );

        if (!device.isEnabled()) {
            throw new IllegalArgumentException("Appareil désactivé");
        }

        // 4. Ignorer les messages déjà traités
        if (processedMessageRepository.existsByMessageId(message.getMessageId())) {
            return "Message déjà traité";
        }

        // 5. Vérifier toutes les mesures AVANT de les enregistrer
        List<MeasurementType> types = new ArrayList<>();

        for (MeasurementInput input : message.getMeasurements()) {

            MeasurementType type = measurementTypeRepository
                    .findByTypeAndEnabledTrue(input.getType())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Type de mesure non autorisé : " + input.getType()
                            )
                    );

            if (!type.getUnit().equals(input.getUnit())) {
                throw new IllegalArgumentException(
                        "Unité incorrecte pour " + input.getType()
                );
            }

            Double value = input.getValue();

            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Valeur de mesure invalide");
            }

            if (type.getMinValue() != null
                    && value < type.getMinValue()) {
                throw new IllegalArgumentException(
                        "Valeur inférieure à la limite autorisée pour " + input.getType()
                );
            }

            if (type.getMaxValue() != null
                    && value > type.getMaxValue()) {
                throw new IllegalArgumentException(
                        "Valeur supérieure à la limite autorisée pour " + input.getType()
                );
            }

            types.add(type);
        }

        // 6. Enregistrer les mesures après validation complète
        LocalDateTime receivedAt = LocalDateTime.now();

        for (int i = 0; i < message.getMeasurements().size(); i++) {

            MeasurementInput input = message.getMeasurements().get(i);

            Measurement measurement = new Measurement();

            measurement.setDevice(device);
            measurement.setMessageId(message.getMessageId());
            measurement.setType(input.getType());
            measurement.setValue(input.getValue());
            measurement.setUnit(input.getUnit());
            measurement.setMeasuredAt(message.getTimestamp());
            measurement.setReceivedAt(receivedAt);

            measurementRepository.save(measurement);
        }

        // 7. Enregistrer l'identifiant du message traité
        ProcessedMessage processedMessage = new ProcessedMessage();

        processedMessage.setMessageId(message.getMessageId());
        processedMessage.setDeviceId(message.getDeviceId());
        processedMessage.setProcessedAt(receivedAt);

        processedMessageRepository.save(processedMessage);

        // 8. Mettre à jour l'état de l'appareil
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(java.time.Instant.now());

        deviceRepository.save(device);

        return "Message traité avec succès";
    }
}