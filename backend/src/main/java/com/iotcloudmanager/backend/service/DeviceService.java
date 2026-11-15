package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.dto.DeviceRequest;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Device getDeviceById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appareil introuvable"));
    }

    public Device getDeviceByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Appareil introuvable"));
    }

    @Transactional
    public Device createDevice(DeviceRequest request) {

        if (deviceRepository.existsByDeviceId(request.getDeviceId())) {
            throw new RuntimeException("Cet identifiant d'appareil existe déjà");
        }

        Device device = new Device();

        device.setDeviceId(request.getDeviceId());
        device.setName(request.getName());
        device.setIntervalSec(
                request.getIntervalSec() == null ? 60 : request.getIntervalSec()
        );
        device.setEnabled(true);
        device.setStatus(DeviceStatus.UNKNOWN);

        return deviceRepository.save(device);
    }

    @Transactional
    public Device updateDevice(Long id, DeviceRequest request) {

        Device device = getDeviceById(id);

        if (!device.getDeviceId().equals(request.getDeviceId())
                && deviceRepository.existsByDeviceId(request.getDeviceId())) {
            throw new RuntimeException("Cet identifiant est déjà utilisé");
        }

        device.setDeviceId(request.getDeviceId());
        device.setName(request.getName());

        if (request.getIntervalSec() != null) {
            device.setIntervalSec(request.getIntervalSec());
        }

        return deviceRepository.save(device);
    }

    @Transactional
    public Device changeEnabled(Long id, boolean enabled) {

        Device device = getDeviceById(id);
        device.setEnabled(enabled);

        if (!enabled) {
            device.setStatus(DeviceStatus.OFFLINE);
        } else {
            device.setStatus(DeviceStatus.UNKNOWN);
        }

        return deviceRepository.save(device);
    }

    @Transactional
    public void deleteDevice(Long id) {

        Device device = getDeviceById(id);

        deviceRepository.delete(device);
    }
}