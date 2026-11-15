package com.iotcloudmanager.backend.service;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class MqttSubscriberService {

    private MqttClient client;
    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String CLIENT_ID = "iot-cloud-manager-backend-02";

    @PostConstruct
    public void connectAndSubscribe() {
        try {
            client = new MqttClient(BROKER_URL, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false); // Garde la session active (requis par le contrat)
            options.setAutomaticReconnect(true);

            client.connect(options);
            System.out.println("✅ Connecté au broker MQTT Mosquitto avec succès !");

            // Abonnement aux topics demandés avec QoS 1
            String[] topics = {
                "iot/devices/+/telemetry",
                "iot/devices/+/status"
            };
            int[] qos = {1, 1};

            client.subscribe(topics, qos);
            System.out.println("📡 Abonné aux topics MQTT avec QoS 1");

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("⚠️ Connexion MQTT perdue : " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    validateAndProcessMessage(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Non utilisé pour la réception
                }
            });

        } catch (MqttException e) {
            System.err.println("❌ Erreur de connexion MQTT : " + e.getMessage());
        }
    }

    private void validateAndProcessMessage(String topic, String payload) {
        System.out.println("\n--- [MQTT] Message reçu sur le topic : " + topic + " ---");
        
        // 1. Validation de la taille (< 1 Ko)
        if (payload.getBytes().length > 1024) {
            System.err.println("❌ Rejeté : Message trop volumineux (> 1 Ko)");
            return;
        }

        // 2. Affichage et validation basique du contenu JSON reçu
        System.out.println("📦 Payload JSON : " + payload);
        System.out.println("✔ Message validé avec succès par le backend !");
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                System.out.println("🔌 Déconnecté du broker MQTT.");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}