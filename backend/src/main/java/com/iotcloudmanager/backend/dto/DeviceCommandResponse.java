package com.iotcloudmanager.backend.dto;

import com.iotcloudmanager.backend.entity.DeviceCommand;
import com.iotcloudmanager.backend.entity.DeviceCommandStatus;

import java.time.Instant;
import java.util.UUID;

public record DeviceCommandResponse(
        UUID commandId,
        String deviceId,
        String type,
        Object params,
        DeviceCommandStatus status,
        Instant createdAt,
        Instant acknowledgedAt
) {
    public static DeviceCommandResponse from(DeviceCommand command, Object params) {
        return new DeviceCommandResponse(
                command.getCommandId(),
                command.getDevice().getDeviceId(),
                command.getType(),
                params,
                command.getStatus(),
                command.getCreatedAt(),
                command.getAcknowledgedAt()
        );
    }
}
