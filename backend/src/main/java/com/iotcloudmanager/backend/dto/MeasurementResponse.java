
package com.iotcloudmanager.backend.dto;

import com.iotcloudmanager.backend.entity.Measurement;

import java.time.LocalDateTime;

public class MeasurementResponse {

    private Long id;
    private String deviceId;
    private String messageId;
    private String type;
    private Double value;
    private String unit;
    private LocalDateTime measuredAt;
    private LocalDateTime receivedAt;

    public MeasurementResponse() {
    }

    public static MeasurementResponse from(Measurement measurement) {

        MeasurementResponse response = new MeasurementResponse();

        response.setId(measurement.getId());
        response.setDeviceId(measurement.getDevice().getDeviceId());
        response.setMessageId(measurement.getMessageId());
        response.setType(measurement.getType());
        response.setValue(measurement.getValue());
        response.setUnit(measurement.getUnit());
        response.setMeasuredAt(measurement.getMeasuredAt());
        response.setReceivedAt(measurement.getReceivedAt());

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(LocalDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
