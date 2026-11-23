package com.iotcloudmanager.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryMessage {

    @NotNull
    private Integer schemaVersion;

    @NotBlank
    @Pattern(
        regexp = "^[a-z0-9-]{3,50}$",
        message = "deviceId doit contenir entre 3 et 50 caractères minuscules, chiffres ou tirets"
    )
    private String deviceId;

    @NotBlank
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
        message = "messageId doit être un UUID valide"
    )
    private String messageId;

    @NotNull
    private Instant timestamp;

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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<MeasurementInput> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<MeasurementInput> measurements) {
        this.measurements = measurements;
    }
}
