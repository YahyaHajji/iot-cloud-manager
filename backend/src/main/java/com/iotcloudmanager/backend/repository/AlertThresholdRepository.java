
package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.AlertThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertThresholdRepository
        extends JpaRepository<AlertThreshold, Long> {

    Optional<AlertThreshold> findByDeviceDeviceIdAndTypeAndEnabledTrue(
            String deviceId, String type
    );

    @Query("SELECT t FROM AlertThreshold t JOIN FETCH t.device WHERE t.device.deviceId = :deviceId")
    List<AlertThreshold> findByDeviceDeviceId(@Param("deviceId") String deviceId);

    @Query("SELECT t FROM AlertThreshold t JOIN FETCH t.device")
    List<AlertThreshold> findAllWithDevice();
}
