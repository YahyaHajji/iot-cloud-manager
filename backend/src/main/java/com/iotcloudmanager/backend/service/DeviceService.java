package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.dto.DeviceRequest;
import com.iotcloudmanager.backend.dto.DeviceResponse;
import com.iotcloudmanager.backend.dto.LastMeasurementResponse;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import com.iotcloudmanager.backend.repository.MeasurementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final MeasurementRepository measurementRepository;

    public DeviceService(
            DeviceRepository deviceRepository,
            MeasurementRepository measurementRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.measurementRepository = measurementRepository;
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {

        return deviceRepository.findAll()
                .stream()
                .map(device -> {

                    Map<String, LastMeasurementResponse> latestByType =
                            new LinkedHashMap<>();

                    measurementRepository
                            .findByDeviceDeviceIdOrderByMeasuredAtDesc(
                                    device.getDeviceId()
                            )
                            .forEach(measurement ->
                                    latestByType.putIfAbsent(
                                            measurement.getType(),
                                            LastMeasurementResponse.from(measurement)
                                    )
                            );

                    return DeviceResponse.from(
                            device,
                            List.copyOf(latestByType.values())
                    );
                })
                .toList();
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
        device.setLocation(request.getLocation());
        device.setDescription(request.getDescription());
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

        device.setLocation(request.getLocation());
        device.setDescription(request.getDescription());

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