
package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.Measurement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDateTime;
import java.util.List;

public interface MeasurementRepository
        extends JpaRepository<Measurement, Long> {

    @EntityGraph(attributePaths = "device")
    List<Measurement> findByDeviceDeviceIdOrderByMeasuredAtDesc(
            String deviceId
    );

    @EntityGraph(attributePaths = "device")
    List<Measurement> findByDeviceDeviceIdAndTypeOrderByMeasuredAtDesc(
            String deviceId,
            String type
    );

    @EntityGraph(attributePaths = "device")
    List<Measurement> findByDeviceDeviceIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
            String deviceId,
            LocalDateTime start,
            LocalDateTime end
    );

    @EntityGraph(attributePaths = "device")
    List<Measurement> findTop100ByDeviceDeviceIdOrderByMeasuredAtDesc(
            String deviceId
    );
}
