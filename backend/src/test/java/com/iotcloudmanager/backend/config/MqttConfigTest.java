package com.iotcloudmanager.backend.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MqttConfigTest {

    @Test
    void shouldAcceptAnSslBrokerUri() throws MqttException {
        MqttConfig config = new MqttConfig();
        ReflectionTestUtils.setField(
                config, "brokerUrl", "ssl://iot-manager-yahya.duckdns.org:8883");

        MqttConnectOptions options = config.mqttConnectOptions();

        assertArrayEquals(
                new String[] {"ssl://iot-manager-yahya.duckdns.org:8883"},
                options.getServerURIs());

        MqttAsyncClient client = new MqttAsyncClient(
                options.getServerURIs()[0],
                "ssl-uri-check",
                new MemoryPersistence());
        assertNotNull(client);
        client.close();
    }
}
