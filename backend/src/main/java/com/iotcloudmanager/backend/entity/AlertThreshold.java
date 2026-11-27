
package com.iotcloudmanager.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "alert_thresholds",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_alert_threshold_device_type",
        columnNames = {"device_id", "measurement_type"}
    )
)
public class AlertThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "measurement_type", nullable = false, length = 50)
    private String type;

    @Column(name = "min_threshold")
    private Double minThreshold;

    @Column(name = "max_threshold")
    private Double maxThreshold;

    @Column(nullable = false)
    private boolean enabled = true;

    public AlertThreshold() {
    }

    public Long getId() {
        return id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
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
