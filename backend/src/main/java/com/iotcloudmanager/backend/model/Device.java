package com.iotcloudmanager.backend.model;

public class Device {
    private String deviceId;
    private String name;
    private boolean active;
    private String type;
    private String unit;
    private double minValue;
    private double maxValue;

    // Constructeurs
    public Device() {}

    public Device(String deviceId, String name, boolean active, String type, String unit, double minValue, double maxValue) {
        this.deviceId = deviceId;
        this.name = name;
        this.active = active;
        this.type = type;
        this.unit = unit;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    // Getters et Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getMinValue() { return minValue; }
    public void setMinValue(double minValue) { this.minValue = minValue; }

    public double getMaxValue() { return maxValue; }
    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
}