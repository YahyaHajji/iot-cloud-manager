package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findByDeviceDeviceIdOrderByMeasuredAtDesc(String deviceId);

    List<Measurement> findByDeviceDeviceIdAndTypeOrderByMeasuredAtDesc(
            String deviceId,
            String type
    );

    List<Measurement> findByDeviceDeviceIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
            String deviceId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Measurement> findTop100ByDeviceDeviceIdOrderByMeasuredAtDesc(String deviceId);
}