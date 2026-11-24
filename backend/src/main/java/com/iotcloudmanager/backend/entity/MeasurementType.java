
package com.iotcloudmanager.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "measurement_types")
public class MeasurementType {

    @Id
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(nullable = false)
    private boolean enabled = true;

    public MeasurementType() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getMinValue() {
        return minValue;
    }

    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
