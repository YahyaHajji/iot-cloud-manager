package com.iotcloudmanager.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class TelemetryMessage {

    @NotNull
    private Integer schemaVersion;

    @NotBlank
    private String deviceId;

    @NotBlank
    private String messageId;

    @NotNull
    private LocalDateTime timestamp;

    @NotNull
    @Size(min = 1, max = 10)
    @Valid
    private List<MeasurementInput> measurements;

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<MeasurementInput> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<MeasurementInput> measurements) {
        this.measurements = measurements;
    }
}