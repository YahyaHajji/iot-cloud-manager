
package com.iotcloudmanager.backend.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iotcloudmanager.backend.dto.DeviceCommandAcknowledgement;
import com.iotcloudmanager.backend.dto.DeviceStatusMessage;
import com.iotcloudmanager.backend.dto.TelemetryMessage;
import com.iotcloudmanager.backend.service.DeviceCommandService;
import com.iotcloudmanager.backend.service.DeviceStatusService;
import com.iotcloudmanager.backend.service.MessageRejectedException;
import com.iotcloudmanager.backend.service.TelemetryService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class MqttSubscriber implements MqttCallbackExtended {

    private static final Logger log =
            LoggerFactory.getLogger(MqttSubscriber.class);

    private final TelemetryService telemetryService;
    private final DeviceStatusService deviceStatusService;
    private final DeviceCommandService deviceCommandService;
    private final ObjectMapper objectMapper;
    private final MqttConnectOptions connectOptions;
    private final Validator validator;
    private final MeterRegistry meterRegistry;

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.enabled:true}")
    private boolean mqttEnabled;

    private MqttClient client;

    public MqttSubscriber(
            TelemetryService telemetryService,
            DeviceStatusService deviceStatusService,
            DeviceCommandService deviceCommandService,
            ObjectMapper objectMapper,
            MqttConnectOptions connectOptions,
            Validator validator,
            MeterRegistry meterRegistry) {

        this.telemetryService = telemetryService;
        this.deviceStatusService = deviceStatusService;
        this.deviceCommandService = deviceCommandService;
        this.objectMapper = objectMapper;
        this.connectOptions = connectOptions;
        this.validator = validator;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void connect() {

        if (!mqttEnabled) {
            log.info("Connexion MQTT désactivée par configuration.");
            return;
        }

        try {
            client = new MqttClient(
                    brokerUrl,
                    clientId,
                    new org.eclipse.paho.client.mqttv3.persist.MemoryPersistence()
            );

            client.setCallback(this);
            client.connect(connectOptions);

            subscribeToTopics();

            log.info("Connexion MQTT réussie au broker {}.", brokerUrl);

        } catch (MqttException e) {
            log.error("Impossible de se connecter au broker MQTT.", e);
        }
    }

    private void subscribeToTopics() {

        if (client == null || !client.isConnected()) {
            log.warn("Abonnement MQTT impossible : client non connecté.");
            return;
        }

        try {
            client.subscribe("iot/devices/+/telemetry", 1);
            client.subscribe("iot/devices/+/status", 1);
            client.subscribe("iot/devices/+/commands/ack", 1);

            log.info("Abonnements MQTT actifs : telemetry, status et command ack.");

        } catch (MqttException e) {
            log.error("Erreur pendant les abonnements MQTT.", e);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {

        if (reconnect) {
            log.info("Reconnexion MQTT réussie à {}.", serverURI);
            subscribeToTopics();
        } else {
            log.info("Connexion MQTT initialisée à {}.", serverURI);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {

        log.warn(
                "Connexion MQTT perdue. Reconnexion automatique activée. Cause : {}",
                cause == null ? "inconnue" : cause.getMessage()
        );
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {

        try {
            byte[] payload = mqttMessage.getPayload();

            if (payload == null || payload.length == 0) {
                reject("empty_payload", "Message MQTT vide");
                return;
            }

            if (payload.length > 1024) {
                reject("payload_too_large", "Message supérieur à 1 Ko");
                return;
            }

            String[] parts = topic.split("/", -1);

            if (parts.length == 5
                    && "iot".equals(parts[0])
                    && "devices".equals(parts[1])
                    && "commands".equals(parts[3])
                    && "ack".equals(parts[4])) {

                String deviceId = parts[2];
                if (!deviceId.matches("^[a-z0-9-]{3,50}$")) {
                    reject("invalid_device_id", "Identifiant invalide dans l'accusé");
                    return;
                }

                String content = new String(payload, StandardCharsets.UTF_8);
                DeviceCommandAcknowledgement acknowledgement =
                        objectMapper.readValue(
                                content,
                                DeviceCommandAcknowledgement.class
                        );

                if (!deviceId.equals(acknowledgement.getDeviceId())) {
                    reject("invalid_message",
                            "Le deviceId de l'accusé ne correspond pas au topic");
                    return;
                }

                if (!isValid(acknowledgement)) {
                    reject("invalid_message",
                            "Validation du message command ack échouée");
                    return;
                }

                deviceCommandService.processAcknowledgement(acknowledgement);
                return;
            }

            if (parts.length != 4
                    || !"iot".equals(parts[0])
                    || !"devices".equals(parts[1])
                    || parts[2].isBlank()) {

                reject("invalid_topic", "Topic MQTT invalide");
                return;
            }

            String deviceId = parts[2];

            if (!deviceId.matches("^[a-z0-9-]{3,50}$")) {
                reject(
                        "invalid_device_id",
                        "Identifiant invalide dans le topic MQTT"
                );
                return;
            }

            String messageType = parts[3];

            String content = new String(payload, StandardCharsets.UTF_8);

            if ("telemetry".equals(messageType)) {

                TelemetryMessage message =
                        objectMapper.readValue(content, TelemetryMessage.class);

                if (!isValid(message)) {
                    reject(
                            "invalid_message",
                            "Validation du message telemetry échouée"
                    );
                    return;
                }

                String result =
                        telemetryService.processTelemetry(deviceId, message);

                recordResult(
                        messageType,
                        TelemetryService.RESULT_DUPLICATE.equals(result)
                );

            } else if ("status".equals(messageType)) {

                DeviceStatusMessage message =
                        objectMapper.readValue(content, DeviceStatusMessage.class);

                if (!isValid(message)) {
                    reject(
                            "invalid_message",
                            "Validation du message status échouée"
                    );
                    return;
                }

                String result =
                        deviceStatusService.processStatus(deviceId, message);

                recordResult(
                        messageType,
                        DeviceStatusService.RESULT_DUPLICATE.equals(result)
                );

            } else {
                reject("invalid_message_type", "Type de message MQTT inconnu");
            }

        } catch (JsonProcessingException e) {
            reject("invalid_json", "Le contenu JSON est invalide");

        } catch (MessageRejectedException e) {
            reject(e.getReason(), e.getMessage());

        } catch (IllegalArgumentException e) {
            reject("processing_error", e.getMessage());

        } catch (Exception e) {
            log.error("Erreur inattendue pendant le traitement MQTT.", e);
            reject("processing_error", "Erreur interne de traitement");
        }
    }

    private <T> boolean isValid(T message) {

        Set<ConstraintViolation<T>> violations =
                validator.validate(message);

        if (!violations.isEmpty()) {

            violations.forEach(v ->
                    log.warn(
                            "Validation MQTT refusée : {} - {}",
                            v.getPropertyPath(),
                            v.getMessage()
                    )
            );

            return false;
        }

        return true;
    }

    /**
     * Compte un message traité : accepté s'il est nouveau, doublon sinon.
     * Le compteur « duplicate » évite de compter deux fois un même message
     * dans le débit de télémétrie.
     */
    private void recordResult(String messageType, boolean duplicate) {

        if (duplicate) {
            meterRegistry.counter(
                    "mqtt.messages.duplicate",
                    "type",
                    messageType
            ).increment();

            log.info("Message MQTT {} déjà traité, ignoré.", messageType);
            return;
        }

        meterRegistry.counter(
                "mqtt.messages.accepted",
                "type",
                messageType
        ).increment();
    }

    private void reject(String reason, String detail) {

        meterRegistry.counter(
                "mqtt.messages.rejected",
                "reason",
                reason
        ).increment();

        log.warn("Message MQTT rejeté [{}] : {}", reason, detail);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Aucun message sortant n'est publié par ce composant.
    }

    @PreDestroy
    public void disconnect() {

        try {
            if (client != null) {

                if (client.isConnected()) {
                    client.disconnect();
                }

                client.close();
                log.info("Client MQTT déconnecté proprement.");
            }

        } catch (MqttException e) {
            log.error("Erreur pendant la déconnexion MQTT.", e);
        }
    }
}
