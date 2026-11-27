
package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.entity.Alert;
import com.iotcloudmanager.backend.entity.AlertThreshold;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.repository.AlertRepository;
import com.iotcloudmanager.backend.repository.AlertThresholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertThresholdRepository thresholdRepository;

    public AlertService(
            AlertRepository alertRepository,
            AlertThresholdRepository thresholdRepository
    ) {
        this.alertRepository = alertRepository;
        this.thresholdRepository = thresholdRepository;
    }

    @Transactional
    public void checkMeasurement(
            Device device,
            String type,
            Double value,
            String unit
    ) {
        AlertThreshold threshold = thresholdRepository
                .findByDeviceDeviceIdAndTypeAndEnabledTrue(
                        device.getDeviceId(), type
                )
                .orElse(null);

        if (threshold == null) {
            return;
        }

        boolean belowMinimum =
                threshold.getMinThreshold() != null
                && value < threshold.getMinThreshold();

        boolean aboveMaximum =
                threshold.getMaxThreshold() != null
                && value > threshold.getMaxThreshold();

        if (!belowMinimum && !aboveMaximum) {
            return;
        }

        Alert alert = new Alert();
        alert.setDevice(device);
        alert.setType(type);
        alert.setValue(value);
        alert.setUnit(unit);
        alert.setSeverity("WARNING");
        alert.setTriggeredAt(LocalDateTime.now());

        alertRepository.save(alert);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByTriggeredAtDesc();
    }

    public List<Alert> getDeviceAlerts(String deviceId) {
        return alertRepository
                .findByDeviceDeviceIdOrderByTriggeredAtDesc(deviceId);
    }
}
