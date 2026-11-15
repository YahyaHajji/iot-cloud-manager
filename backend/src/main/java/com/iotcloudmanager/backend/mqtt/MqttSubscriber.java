
package com.iotcloudmanager.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iotcloudmanager.backend.dto.TelemetryMessage;
import com.iotcloudmanager.backend.service.TelemetryService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttSubscriber implements MqttCallback {

    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;
    private final MqttConnectOptions connectOptions;

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    private MqttClient client;

    public MqttSubscriber(
            TelemetryService telemetryService,
            ObjectMapper objectMapper,
            MqttConnectOptions connectOptions
    ) {
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
        this.connectOptions = connectOptions;
    }

    @PostConstruct
    public void connect() {

        try {
            client = new MqttClient(
                    brokerUrl,
                    clientId,
                    new org.eclipse.paho.client.mqttv3.persist.MemoryPersistence()
            );

            client.setCallback(this);
            client.connect(connectOptions);

            client.subscribe("iot/devices/+/telemetry", 1);
            client.subscribe("iot/devices/+/status", 1);

            System.out.println("MQTT connecté avec succès !");
            System.out.println("Abonné aux topics telemetry et status.");

        } catch (MqttException e) {
            System.err.println("Erreur de connexion MQTT : " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Connexion MQTT perdue : " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {

        try {

            byte[] payload = mqttMessage.getPayload();

            if (payload.length > 1024) {
                throw new IllegalArgumentException(
                        "Message MQTT supérieur à 1 Ko"
                );
            }

            String content = new String(payload, StandardCharsets.UTF_8);

            System.out.println("Message MQTT reçu sur : " + topic);
            System.out.println("Contenu : " + content);

            String[] parts = topic.split("/");

            if (parts.length != 4
                    || !"iot".equals(parts[0])
                    || !"devices".equals(parts[1])) {
                throw new IllegalArgumentException("Topic MQTT invalide");
            }

            String deviceId = parts[2];
            String messageType = parts[3];

            if ("telemetry".equals(messageType)) {

                TelemetryMessage message =
                        objectMapper.readValue(content, TelemetryMessage.class);

                String result = telemetryService.processTelemetry(
                        deviceId,
                        message
                );

                System.out.println(result);

            } else if ("status".equals(messageType)) {

                System.out.println(
                        "Message de statut reçu pour : " + deviceId
                );

                // Le traitement du statut sera ajouté dans la suite.
            }

        } catch (Exception e) {

            System.err.println(
                    "Message MQTT rejeté : " + e.getMessage()
            );
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Aucun message sortant pour le moment.
    }

    @PreDestroy
    public void disconnect() {

        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            System.err.println(
                    "Erreur lors de la déconnexion MQTT : " + e.getMessage()
            );
        }
    }
}
