package com.iotcloudmanager.backend.dto;

import com.iotcloudmanager.backend.entity.Measurement;
import java.time.LocalDateTime;

public class LastMeasurementResponse {

    private String type;
    private Double value;
    private String unit;
    private LocalDateTime timestamp;

    public LastMeasurementResponse() {
    }

    public static LastMeasurementResponse from(Measurement measurement) {
        LastMeasurementResponse response = new LastMeasurementResponse();
        response.setType(measurement.getType());
        response.setValue(measurement.getValue());
        response.setUnit(measurement.getUnit());
        response.setTimestamp(measurement.getMeasuredAt());
        return response;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}