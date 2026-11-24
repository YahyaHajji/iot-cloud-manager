package com.iotcloudmanager.backend.dto;

import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;

import java.time.Instant;
import java.util.List;

public class DeviceResponse {

    private Long id;
    private String deviceId;
    private String name;
    private DeviceStatus status;
    private boolean enabled;
    private Integer intervalSec;
    private Instant lastSeenAt;
    private String firmware;
    private String location;
    private String description;
    private List<LastMeasurementResponse> lastMeasurements;

    public static DeviceResponse from(
            Device device,
            List<LastMeasurementResponse> lastMeasurements
    ) {
        DeviceResponse response = new DeviceResponse();
        response.id = device.getId();
        response.deviceId = device.getDeviceId();
        response.name = device.getName();
        response.status = device.getStatus();
        response.enabled = device.isEnabled();
        response.intervalSec = device.getIntervalSec();
        response.lastSeenAt = device.getLastSeenAt();
        response.firmware = device.getFirmware();
        response.location = device.getLocation();
        response.description = device.getDescription();
        response.lastMeasurements = lastMeasurements;
        return response;
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public DeviceStatus getStatus() { return status; }
    public boolean isEnabled() { return enabled; }
    public Integer getIntervalSec() { return intervalSec; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public String getFirmware() { return firmware; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public List<LastMeasurementResponse> getLastMeasurements() {
        return lastMeasurements;
    }
}