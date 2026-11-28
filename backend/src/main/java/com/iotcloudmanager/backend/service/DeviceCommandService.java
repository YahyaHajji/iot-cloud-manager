package com.iotcloudmanager.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iotcloudmanager.backend.dto.CreateDeviceCommandRequest;
import com.iotcloudmanager.backend.dto.DeviceCommandAcknowledgement;
import com.iotcloudmanager.backend.dto.DeviceCommandResponse;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceCommand;
import com.iotcloudmanager.backend.entity.DeviceCommandStatus;
import com.iotcloudmanager.backend.mqtt.MqttCommandPublisher;
import com.iotcloudmanager.backend.repository.DeviceCommandRepository;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeviceCommandService {

    private final DeviceRepository deviceRepository;
    private final DeviceCommandRepository commandRepository;
    private final MqttCommandPublisher mqttCommandPublisher;
    private final ObjectMapper objectMapper;

    public DeviceCommandService(
            DeviceRepository deviceRepository,
            DeviceCommandRepository commandRepository,
            MqttCommandPublisher mqttCommandPublisher,
            ObjectMapper objectMapper
    ) {
        this.deviceRepository = deviceRepository;
        this.commandRepository = commandRepository;
        this.mqttCommandPublisher = mqttCommandPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeviceCommandResponse createCommand(
            Long deviceId,
            CreateDeviceCommandRequest request
    ) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Appareil introuvable"));

        validateCommand(request);

        DeviceCommand command = new DeviceCommand();
        command.setCommandId(UUID.randomUUID());
        command.setDevice(device);
        command.setType(request.getType());
        command.setParams(writeParams(request.getParams()));
        command.setStatus(DeviceCommandStatus.PENDING);
        command.setCreatedAt(Instant.now());

        DeviceCommand saved = commandRepository.save(command);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("commandId", saved.getCommandId());
        payload.put("type", saved.getType());
        payload.put("params", request.getParams());
        payload.put("timestamp", saved.getCreatedAt());

        mqttCommandPublisher.publish(
                "iot/devices/" + device.getDeviceId() + "/commands",
                payload
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DeviceCommandResponse> getCommandHistory(String deviceId) {
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw new IllegalArgumentException("Appareil introuvable");
        }

        return commandRepository.findByDeviceDeviceIdOrderByCreatedAtDesc(deviceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeviceCommandResponse> getCommandHistoryByNumericId(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Appareil introuvable"));
        return getCommandHistory(device.getDeviceId());
    }

    @Transactional
    public void processAcknowledgement(DeviceCommandAcknowledgement acknowledgement) {
        if (acknowledgement.getSchemaVersion() == null
                || acknowledgement.getSchemaVersion() != 1) {
            throw new IllegalArgumentException("Version du schéma invalide");
        }
        if (acknowledgement.getCommandId() == null) {
            throw new IllegalArgumentException("commandId invalide");
        }
        if (!"ok".equals(acknowledgement.getResult())
                && !"error".equals(acknowledgement.getResult())) {
            throw new IllegalArgumentException("result invalide");
        }

        DeviceCommand command = commandRepository
                .findByCommandId(acknowledgement.getCommandId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Commande inconnue"));

        if (!command.getDevice().getDeviceId()
                .equals(acknowledgement.getDeviceId())) {
            throw new IllegalArgumentException(
                    "Le deviceId ne correspond pas à la commande");
        }
        if (command.getStatus() != DeviceCommandStatus.PENDING) {
            return;
        }

        command.setAcknowledgedAt(
                acknowledgement.getTimestamp() == null
                        ? Instant.now()
                        : acknowledgement.getTimestamp()
        );
        command.setStatus("ok".equals(acknowledgement.getResult())
                ? DeviceCommandStatus.ACKNOWLEDGED
                : DeviceCommandStatus.FAILED);

        if ("ok".equals(acknowledgement.getResult())) {
            applySuccessfulCommand(command);
        }

        commandRepository.save(command);
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void markTimedOutCommands() {
        Instant timeoutBefore = Instant.now().minusSeconds(10);
        List<DeviceCommand> timedOut = commandRepository
                .findByStatusAndCreatedAtBefore(
                        DeviceCommandStatus.PENDING,
                        timeoutBefore
                );

        for (DeviceCommand command : timedOut) {
            command.setStatus(DeviceCommandStatus.TIMEOUT);
            command.setAcknowledgedAt(Instant.now());
        }

        commandRepository.saveAll(timedOut);
    }

    private void validateCommand(CreateDeviceCommandRequest request) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("Type de commande obligatoire");
        }

        Map<String, Object> params = request.getParams();
        switch (request.getType()) {
            case "set_interval" -> {
                Object interval = params.get("intervalSec");
                if (!(interval instanceof Number number)
                        || number.doubleValue() < 1
                        || number.doubleValue() > 3600
                        || number.doubleValue() % 1 != 0) {
                    throw new IllegalArgumentException(
                            "intervalSec doit être compris entre 1 et 3600");
                }
            }
            case "enable", "disable", "ping" -> {
                if (!params.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Cette commande ne prend aucun paramètre");
                }
            }
            default -> throw new IllegalArgumentException(
                    "Type de commande inconnu");
        }
    }

    private void applySuccessfulCommand(DeviceCommand command) {
        Map<String, Object> params = readParams(command.getParams());

        switch (command.getType()) {
            case "set_interval" -> {
                Object interval = params.get("intervalSec");
                if (interval instanceof Number number) {
                    command.getDevice().setIntervalSec(number.intValue());
                }
            }
            case "enable" -> command.getDevice().setEnabled(true);
            case "disable" -> command.getDevice().setEnabled(false);
            default -> {
            }
        }

        deviceRepository.save(command.getDevice());
    }

    private String writeParams(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Paramètres invalides");
        }
    }

    private Map<String, Object> readParams(String params) {
        try {
            return objectMapper.readValue(
                    params,
                    objectMapper.getTypeFactory()
                            .constructMapType(Map.class, String.class, Object.class)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Paramètres stockés invalides");
        }
    }

    private DeviceCommandResponse toResponse(DeviceCommand command) {
        return DeviceCommandResponse.from(
                command,
                readParams(command.getParams())
        );
    }
}
