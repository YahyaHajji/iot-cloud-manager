package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.DeviceCommand;
import com.iotcloudmanager.backend.entity.DeviceCommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    Optional<DeviceCommand> findByCommandId(UUID commandId);

    List<DeviceCommand> findByDeviceDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<DeviceCommand> findByStatusAndCreatedAtBefore(
            DeviceCommandStatus status,
            Instant createdBefore
    );
}
