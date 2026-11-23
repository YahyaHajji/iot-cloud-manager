package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DeviceOfflineScheduler {

    private final DeviceRepository deviceRepository;

    public DeviceOfflineScheduler(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void detectOfflineDevices() {

        Instant now = Instant.now();

        for (Device device : deviceRepository.findByEnabledTrue()) {

            if (device.getLastSeenAt() == null
                    || device.getIntervalSec() == null
                    || device.getIntervalSec() <= 0) {
                continue;
            }

            long timeoutSeconds = 3L * device.getIntervalSec();

            if (device.getLastSeenAt().plusSeconds(timeoutSeconds).isBefore(now)
                    && device.getStatus() != DeviceStatus.OFFLINE) {

                device.setStatus(DeviceStatus.OFFLINE);
                deviceRepository.save(device);

                System.out.println(
                        "Capteur passé OFFLINE : " + device.getDeviceId()
                );
            }
        }
    }
}
