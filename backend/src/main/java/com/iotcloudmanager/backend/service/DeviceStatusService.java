
package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.dto.DeviceStatusMessage;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.entity.ProcessedMessage;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import com.iotcloudmanager.backend.repository.ProcessedMessageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class DeviceStatusService {

    private final DeviceRepository deviceRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    public DeviceStatusService(
            DeviceRepository deviceRepository,
            ProcessedMessageRepository processedMessageRepository) {

        this.deviceRepository = deviceRepository;
        this.processedMessageRepository = processedMessageRepository;
    }

    @Transactional
    public String processStatus(
            String topicDeviceId,
            DeviceStatusMessage message) {

        // 1. Vérifier que le message existe
        if (message == null) {
            throw new IllegalArgumentException(
                    "Message de statut absent");
        }

        // 2. Vérifier la version du contrat MQTT
        if (message.getSchemaVersion() == null
                || message.getSchemaVersion() != 1) {

            throw new IllegalArgumentException(
                    "Version du schéma invalide");
        }

        // 3. Vérifier la correspondance entre topic et message
        if (topicDeviceId == null
                || message.getDeviceId() == null
                || !topicDeviceId.equals(message.getDeviceId())) {

            throw new IllegalArgumentException(
                    "DeviceId invalide");
        }

        // 4. Vérifier le messageId
        if (message.getMessageId() == null
                || message.getMessageId().isBlank()) {

            throw new IllegalArgumentException(
                    "Message de statut incomplet");
        }

        // 5. Éviter les doublons
        if (processedMessageRepository.existsByMessageId(
                message.getMessageId())) {

            return "Message de statut déjà traité";
        }

        // 6. Vérifier que le capteur existe
        Device device = deviceRepository
                .findByDeviceId(topicDeviceId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Appareil non enregistré"));

        // 7. Vérifier que le capteur est activé
        if (!device.isEnabled()) {
            throw new IllegalArgumentException(
                    "Appareil désactivé");
        }

        // 8. Normaliser le statut
        String status = message.getStatus() == null
                ? ""
                : message.getStatus().trim()
                        .toUpperCase(Locale.ROOT);

        // 9. Traitement ONLINE
        if ("ONLINE".equals(status)) {

            if (message.getIntervalSec() != null
                    && message.getIntervalSec() < 1) {

                throw new IllegalArgumentException(
                        "intervalSec invalide");
            }

            device.setStatus(DeviceStatus.ONLINE);

            if (message.getIntervalSec() != null) {
                device.setIntervalSec(
                        message.getIntervalSec());
            }

            if (message.getFirmware() != null
                    && !message.getFirmware().isBlank()) {

                device.setFirmware(
                        message.getFirmware().trim());
            }

        }
        // 10. Traitement OFFLINE
        else if ("OFFLINE".equals(status)) {

            device.setStatus(DeviceStatus.OFFLINE);

        }
        // 11. Statut inconnu
        else {
            throw new IllegalArgumentException(
                    "Statut inconnu : " + status);
        }

        // 12. Enregistrer la dernière activité
        Instant lastSeen = message.getTimestamp() != null
                ? message.getTimestamp()
                : Instant.now();

        device.setLastSeenAt(lastSeen);

        deviceRepository.save(device);

        // 13. Enregistrer le message traité
        ProcessedMessage processed = new ProcessedMessage();

        processed.setMessageId(message.getMessageId());
        processed.setDeviceId(message.getDeviceId());

        processed.setProcessedAt(
                LocalDateTime.ofInstant(
                        Instant.now(),
                        ZoneOffset.UTC));

        processedMessageRepository.save(processed);

        return "Statut mis à jour : " + status;
    }
}
