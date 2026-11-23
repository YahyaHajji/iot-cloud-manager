
package com.iotcloudmanager.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public class AlertThresholdRequest {

    @NotBlank
    private String deviceId;

    @NotBlank
    private String type;

    private Double minThreshold;
    private Double maxThreshold;

    private boolean enabled = true;

    @AssertTrue(message = "Au moins un seuil doit être défini")
    public boolean isThresholdValid() {
        return minThreshold != null || maxThreshold != null;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(Double minThreshold) {
        this.minThreshold = minThreshold;
    }

    public Double getMaxThreshold() {
        return maxThreshold;
    }

    public void setMaxThreshold(Double maxThreshold) {
        this.maxThreshold = maxThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
