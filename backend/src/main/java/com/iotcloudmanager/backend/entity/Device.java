package com.iotcloudmanager.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 50)
    private String deviceId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.UNKNOWN;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "interval_sec", nullable = false)
    private Integer intervalSec = 5;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(length = 100)
    private String firmware;

    @Column(columnDefinition = "TEXT")
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Device() {
    }

    public Long getId() { return id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Integer getIntervalSec() { return intervalSec; }
    public void setIntervalSec(Integer intervalSec) { this.intervalSec = intervalSec; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}