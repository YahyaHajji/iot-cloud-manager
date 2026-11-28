package com.iotcloudmanager.backend.service;

/**
 * Message MQTT refusé avec un motif précis.
 *
 * Le motif devient l'étiquette « reason » du compteur Prometheus
 * mqtt_messages_rejected_total (par exemple device_disabled).
 * L'exception reste une IllegalArgumentException : le code existant qui
 * attrape ce type continue de fonctionner.
 */
public class MessageRejectedException extends IllegalArgumentException {

    private final String reason;

    public MessageRejectedException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
