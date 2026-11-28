package com.iotcloudmanager.backend.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttCommandPublisher.class);

    private final MqttConnectOptions connectOptions;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.publisher.client.id:iot-backend-command-publisher}")
    private String clientId;

    @Value("${mqtt.enabled:true}")
    private boolean mqttEnabled;

    private MqttClient client;

    public MqttCommandPublisher(
            MqttConnectOptions connectOptions,
            ObjectMapper objectMapper
    ) {
        this.connectOptions = connectOptions;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void connect() {
        if (!mqttEnabled) {
            log.info("Publication MQTT des commandes désactivée.");
            return;
        }

        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            client.connect(connectOptions);
        } catch (MqttException e) {
            log.error("Impossible de connecter le publisher MQTT des commandes.", e);
        }
    }

    public void publish(String topic, Object payload) {
        if (client == null || !client.isConnected()) {
            throw new IllegalStateException("Publisher MQTT des commandes indisponible");
        }

        byte[] json;
        try {
            json = objectMapper.writeValueAsString(payload)
                    .getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser la commande MQTT", e);
        }

        try {
            client.publish(topic, json, 1, false);
        } catch (MqttException e) {
            throw new IllegalStateException("Impossible de publier la commande MQTT", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException e) {
            log.warn("Erreur lors de la fermeture du publisher MQTT.", e);
        }
    }
}
