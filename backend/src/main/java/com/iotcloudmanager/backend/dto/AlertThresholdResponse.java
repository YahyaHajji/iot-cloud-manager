
package com.iotcloudmanager.backend.dto;

import com.iotcloudmanager.backend.entity.AlertThreshold;

public record AlertThresholdResponse(
        Long id,
        String deviceId,
        String deviceName,
        String type,
        Double minThreshold,
        Double maxThreshold,
        boolean enabled
) {
    public static AlertThresholdResponse from(AlertThreshold threshold) {
        return new AlertThresholdResponse(
                threshold.getId(),
                threshold.getDevice().getDeviceId(),
                threshold.getDevice().getName(),
                threshold.getType(),
                threshold.getMinThreshold(),
                threshold.getMaxThreshold(),
                threshold.isEnabled()
        );
    }
}
