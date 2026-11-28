package com.iotcloudmanager.backend.service;

import java.time.LocalDateTime;

public record AlertCreatedEvent(
        String deviceId,
        String deviceName,
        String type,
        Double value,
        String unit,
        LocalDateTime triggeredAt
) {
}
