
package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.Measurement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = "device")
    @Query("""
            SELECT measurement
            FROM Measurement measurement
            WHERE measurement.device.deviceId = :deviceId
              AND (:type IS NULL OR measurement.type = :type)
              AND (:start IS NULL OR measurement.measuredAt >= :start)
              AND (:end IS NULL OR measurement.measuredAt <= :end)
            ORDER BY measurement.measuredAt ASC
            """)
    List<Measurement> findForCsvExport(
            @Param("deviceId") String deviceId,
            @Param("type") String type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
