
package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByTriggeredAtDesc();

    List<Alert> findByDeviceDeviceIdOrderByTriggeredAtDesc(String deviceId);
}
