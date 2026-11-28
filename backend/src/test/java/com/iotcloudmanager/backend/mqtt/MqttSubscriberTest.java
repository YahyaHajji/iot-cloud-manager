package com.iotcloudmanager.backend.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.iotcloudmanager.backend.dto.DeviceStatusMessage;
import com.iotcloudmanager.backend.dto.TelemetryMessage;
import com.iotcloudmanager.backend.service.DeviceCommandService;
import com.iotcloudmanager.backend.service.DeviceStatusService;
import com.iotcloudmanager.backend.service.MessageRejectedException;
import com.iotcloudmanager.backend.service.TelemetryService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MqttSubscriberTest {

    private static final String TELEMETRY_TOPIC = "iot/devices/sensor-room-01/telemetry";
    private static final String STATUS_TOPIC = "iot/devices/sensor-room-01/status";
    private static final String COMMAND_ACK_TOPIC =
            "iot/devices/sensor-room-01/commands/ack";

    private static final String TELEMETRY_JSON = """
            {"schemaVersion":1,
             "messageId":"3f1c9a2e-8b7d-4c1a-9e2f-1a2b3c4d5e6f",
             "deviceId":"sensor-room-01",
             "timestamp":"2026-10-28T10:15:30Z",
             "measurements":[{"type":"temperature","value":24.5,"unit":"°C"}]}
            """;

    private static final String STATUS_JSON = """
            {"schemaVersion":1,
             "deviceId":"sensor-room-01",
             "status":"online",
             "timestamp":"2026-10-28T10:00:00Z",
             "intervalSec":5}
            """;

    private static final String STATUS_JSON_WITH_MESSAGE_ID = """
            {"schemaVersion":1,
             "messageId":"4a1c9a2e-8b7d-4c1a-9e2f-1a2b3c4d5e6f",
             "deviceId":"sensor-room-01",
             "status":"online",
             "timestamp":"2026-10-28T10:00:00Z",
             "intervalSec":5}
            """;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private DeviceStatusService deviceStatusService;

    @Mock
    private DeviceCommandService deviceCommandService;

    private SimpleMeterRegistry registry;
    private MqttSubscriber subscriber;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        subscriber = new MqttSubscriber(
                telemetryService,
                deviceStatusService,
                deviceCommandService,
                mapper,
                new MqttConnectOptions(),
                validator,
                registry);
    }

    private void send(String topic, String json) {
        subscriber.messageArrived(topic, new MqttMessage(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private double accepted(String type) {
        var c = registry.find("mqtt.messages.accepted").tag("type", type).counter();
        return c == null ? 0 : c.count();
    }

    private double duplicate(String type) {
        var c = registry.find("mqtt.messages.duplicate").tag("type", type).counter();
        return c == null ? 0 : c.count();
    }

    private double rejected(String reason) {
        var c = registry.find("mqtt.messages.rejected").tag("reason", reason).counter();
        return c == null ? 0 : c.count();
    }

    @Test
    void telemetryNouvelleCompteCommeAcceptee() {
        when(telemetryService.processTelemetry(eq("sensor-room-01"), any(TelemetryMessage.class)))
                .thenReturn(TelemetryService.RESULT_PROCESSED);

        send(TELEMETRY_TOPIC, TELEMETRY_JSON);

        assertEquals(1, accepted("telemetry"));
        assertEquals(0, duplicate("telemetry"));
    }

    @Test
    void telemetryDoublonCompteCommeDoublon() {
        when(telemetryService.processTelemetry(eq("sensor-room-01"), any(TelemetryMessage.class)))
                .thenReturn(TelemetryService.RESULT_DUPLICATE);

        send(TELEMETRY_TOPIC, TELEMETRY_JSON);

        assertEquals(0, accepted("telemetry"));
        assertEquals(1, duplicate("telemetry"));
    }

    @Test
    void statusNouveauCompteCommeAccepte() {
        when(deviceStatusService.processStatus(eq("sensor-room-01"), any(DeviceStatusMessage.class)))
                .thenReturn("Statut traité");

        send(STATUS_TOPIC, STATUS_JSON);

        assertEquals(1, accepted("status"));
    }

    @Test
    void statusDoublonCompteCommeDoublon() {
        when(deviceStatusService.processStatus(eq("sensor-room-01"), any(DeviceStatusMessage.class)))
                .thenReturn(DeviceStatusService.RESULT_DUPLICATE);

        send(STATUS_TOPIC, STATUS_JSON_WITH_MESSAGE_ID);

        assertEquals(1, duplicate("status"));
        assertEquals(0, accepted("status"));
    }

    @Test
    void appareilDesactiveUtiliseLeMotifPrecis() {
        when(telemetryService.processTelemetry(eq("sensor-room-01"), any(TelemetryMessage.class)))
                .thenThrow(new MessageRejectedException("device_disabled", "Appareil désactivé"));

        send(TELEMETRY_TOPIC, TELEMETRY_JSON);

        assertEquals(1, rejected("device_disabled"));
        assertEquals(0, rejected("processing_error"));
        assertEquals(0, accepted("telemetry"));
    }

    @Test
    void appareilInconnuUtiliseLeMotifPrecis() {
        when(telemetryService.processTelemetry(eq("sensor-room-01"), any(TelemetryMessage.class)))
                .thenThrow(new MessageRejectedException("unknown_device", "Appareil non enregistré"));

        send(TELEMETRY_TOPIC, TELEMETRY_JSON);

        assertEquals(1, rejected("unknown_device"));
    }

    @Test
    void erreurGeneriqueDonneProcessingError() {
        when(telemetryService.processTelemetry(eq("sensor-room-01"), any(TelemetryMessage.class)))
                .thenThrow(new IllegalArgumentException("autre"));

        send(TELEMETRY_TOPIC, TELEMETRY_JSON);

        assertEquals(1, rejected("processing_error"));
    }

    @Test
    void jsonInvalideEstRejete() {
        send(TELEMETRY_TOPIC, "{pas du json");

        assertEquals(1, rejected("invalid_json"));
        verify(telemetryService, never()).processTelemetry(any(), any());
    }

    @Test
    void messageInvalideSelonLeSchemaEstRejete() {
        send(TELEMETRY_TOPIC, "{\"schemaVersion\":1,\"deviceId\":\"x\"}");

        assertEquals(1, rejected("invalid_message"));
        verify(telemetryService, never()).processTelemetry(any(), any());
    }

    @Test
    void topicInvalideEtTypeInconnuSontRejetes() {
        send("iot/devices/sensor-room-01", TELEMETRY_JSON);
        send("iot/devices/sensor-room-01/inconnu", TELEMETRY_JSON);

        assertEquals(1, rejected("invalid_topic"));
        assertEquals(1, rejected("invalid_message_type"));
    }

    @Test
    void payloadVideEtTropGrosSontRejetes() {
        subscriber.messageArrived(TELEMETRY_TOPIC, new MqttMessage(new byte[0]));
        subscriber.messageArrived(TELEMETRY_TOPIC, new MqttMessage(new byte[2000]));

        assertEquals(1, rejected("empty_payload"));
        assertEquals(1, rejected("payload_too_large"));
    }

    @Test
    void accuseCommandeValideEstTransmisAuService() {
        String commandId = "a7e4b2c1-5d6f-4e3a-8b9c-0d1e2f3a4b5c";
        send(COMMAND_ACK_TOPIC, """
                {"schemaVersion":1,
                 "commandId":"%s",
                 "deviceId":"sensor-room-01",
                 "result":"ok",
                 "timestamp":"2026-10-28T10:20:01Z"}
                """.formatted(commandId));

        verify(deviceCommandService).processAcknowledgement(any());
    }

    @Test
    void accuseDontLeCapteurNeCorrespondPasAuTopicEstRejete() {
        send(COMMAND_ACK_TOPIC, """
                {"schemaVersion":1,
                 "commandId":"a7e4b2c1-5d6f-4e3a-8b9c-0d1e2f3a4b5c",
                 "deviceId":"sensor-room-02",
                 "result":"ok"}
                """);

        assertEquals(1, rejected("invalid_message"));
        verify(deviceCommandService, never()).processAcknowledgement(any());
    }
}
