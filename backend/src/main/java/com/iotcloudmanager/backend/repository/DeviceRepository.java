package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    List<Device> findByEnabledTrue();

    List<Device> findByStatus(DeviceStatus status);
}
