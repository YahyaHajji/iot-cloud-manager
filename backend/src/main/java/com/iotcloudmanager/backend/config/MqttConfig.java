
package com.iotcloudmanager.backend.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Bean
    public MqttConnectOptions mqttConnectOptions() {

        MqttConnectOptions options = new MqttConnectOptions();

        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        options.setMaxReconnectDelay(30000);

        // Identifiants facultatifs :
        // ils seront utilisés lorsque Mosquitto sera sécurisé.
        if (username != null && !username.isBlank()) {
            options.setUserName(username);
            options.setPassword(
                    password == null ? new char[0] : password.toCharArray()
            );
        }

        return options;
    }
}
