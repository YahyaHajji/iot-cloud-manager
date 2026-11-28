package com.iotcloudmanager.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceCommandAcknowledgement {

    @NotNull
    private Integer schemaVersion;

    @NotNull
    private UUID commandId;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]{3,50}$")
    private String deviceId;

    @NotBlank
    @Pattern(regexp = "^(ok|error)$")
    private String result;
    private String message;
    private Instant timestamp;

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public UUID getCommandId() {
        return commandId;
    }

    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
