package com.iotcloudmanager.backend.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCommandServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceCommandRepository commandRepository;

    @Mock
    private MqttCommandPublisher mqttCommandPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DeviceCommandService newService() {
        return new DeviceCommandService(
                deviceRepository,
                commandRepository,
                mqttCommandPublisher,
                objectMapper
        );
    }

    @Test
    void shouldRejectInvalidSetInterval() {
        Device device = device("sensor-room-01");
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        CreateDeviceCommandRequest request = new CreateDeviceCommandRequest();
        request.setType("set_interval");
        request.setParams(Map.of("intervalSec", 3601));

        assertThrows(
                IllegalArgumentException.class,
                () -> newService().createCommand(1L, request)
        );
    }

    @Test
    void shouldCreateAndPublishCommand() {
        Device device = device("sensor-room-01");
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(commandRepository.save(any(DeviceCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateDeviceCommandRequest request = new CreateDeviceCommandRequest();
        request.setType("set_interval");
        request.setParams(Map.of("intervalSec", 10));

        DeviceCommandResponse response =
                newService().createCommand(1L, request);

        assertEquals("set_interval", response.type());
        assertEquals(DeviceCommandStatus.PENDING, response.status());
        verify(mqttCommandPublisher).publish(
                eq("iot/devices/sensor-room-01/commands"),
                any()
        );
    }

    @Test
    void shouldUpdateIntervalAfterAcknowledgement() {
        Device device = device("sensor-room-01");
        DeviceCommand command = command(device, "set_interval", "{\"intervalSec\":10}");
        when(commandRepository.findByCommandId(command.getCommandId()))
                .thenReturn(Optional.of(command));

        DeviceCommandAcknowledgement ack = new DeviceCommandAcknowledgement();
        ack.setSchemaVersion(1);
        ack.setCommandId(command.getCommandId());
        ack.setDeviceId("sensor-room-01");
        ack.setResult("ok");
        ack.setTimestamp(Instant.now());

        newService().processAcknowledgement(ack);

        assertEquals(DeviceCommandStatus.ACKNOWLEDGED, command.getStatus());
        assertEquals(10, device.getIntervalSec());
        verify(deviceRepository).save(device);
    }

    @Test
    void shouldDisableDeviceAfterSuccessfulAcknowledgement() {
        Device device = device("sensor-room-01");
        DeviceCommand command = command(device, "disable", "{}");
        when(commandRepository.findByCommandId(command.getCommandId()))
                .thenReturn(Optional.of(command));

        DeviceCommandAcknowledgement ack = new DeviceCommandAcknowledgement();
        ack.setSchemaVersion(1);
        ack.setCommandId(command.getCommandId());
        ack.setDeviceId("sensor-room-01");
        ack.setResult("ok");

        newService().processAcknowledgement(ack);

        assertEquals(DeviceCommandStatus.ACKNOWLEDGED, command.getStatus());
        assertFalse(device.isEnabled());
        verify(deviceRepository).save(device);
    }

    @Test
    void shouldMarkPendingCommandAsFailed() {
        Device device = device("sensor-room-01");
        DeviceCommand command = command(device, "ping", "{}");
        when(commandRepository.findByCommandId(command.getCommandId()))
                .thenReturn(Optional.of(command));

        DeviceCommandAcknowledgement ack = new DeviceCommandAcknowledgement();
        ack.setSchemaVersion(1);
        ack.setCommandId(command.getCommandId());
        ack.setDeviceId("sensor-room-01");
        ack.setResult("error");

        newService().processAcknowledgement(ack);

        assertEquals(DeviceCommandStatus.FAILED, command.getStatus());
    }

    @Test
    void shouldTimeOutPendingCommands() {
        Device device = device("sensor-room-01");
        DeviceCommand command = command(device, "ping", "{}");
        command.setCreatedAt(Instant.now().minusSeconds(11));
        when(commandRepository.findByStatusAndCreatedAtBefore(
                eq(DeviceCommandStatus.PENDING),
                any(Instant.class)
        )).thenReturn(List.of(command));

        newService().markTimedOutCommands();

        assertEquals(DeviceCommandStatus.TIMEOUT, command.getStatus());
        verify(commandRepository).saveAll(List.of(command));
    }

    private Device device(String deviceId) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setIntervalSec(5);
        return device;
    }

    private DeviceCommand command(
            Device device,
            String type,
            String params
    ) {
        DeviceCommand command = new DeviceCommand();
        command.setCommandId(UUID.randomUUID());
        command.setDevice(device);
        command.setType(type);
        command.setParams(params);
        command.setStatus(DeviceCommandStatus.PENDING);
        command.setCreatedAt(Instant.now());
        return command;
    }
}
