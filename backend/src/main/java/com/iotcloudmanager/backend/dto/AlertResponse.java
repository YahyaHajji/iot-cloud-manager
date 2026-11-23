
package com.iotcloudmanager.backend.dto;

import com.iotcloudmanager.backend.entity.Alert;
import java.time.LocalDateTime;

public class AlertResponse {

    private Long id;
    private String deviceId;
    private String deviceName;
    private String type;
    private Double value;
    private String unit;
    private String severity;
    private LocalDateTime triggeredAt;

    public static AlertResponse from(Alert alert) {
        AlertResponse response = new AlertResponse();

        response.setId(alert.getId());
        response.setDeviceId(alert.getDevice().getDeviceId());
        response.setDeviceName(alert.getDevice().getName());
        response.setType(alert.getType());
        response.setValue(alert.getValue());
        response.setUnit(alert.getUnit());
        response.setSeverity(alert.getSeverity());
        response.setTriggeredAt(alert.getTriggeredAt());

        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }
}
