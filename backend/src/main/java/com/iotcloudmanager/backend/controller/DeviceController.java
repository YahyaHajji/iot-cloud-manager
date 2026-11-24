package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.dto.DeviceRequest;
import com.iotcloudmanager.backend.dto.DeviceResponse;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.service.DeviceService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Device> getDeviceByDeviceId(
            @PathVariable String deviceId
    ) {
        return ResponseEntity.ok(deviceService.getDeviceByDeviceId(deviceId));
    }

    @PostMapping
    public ResponseEntity<Device> createDevice(
            @Valid @RequestBody DeviceRequest request
    ) {
        Device created = deviceService.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Device> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequest request
    ) {
        return ResponseEntity.ok(deviceService.updateDevice(id, request));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<Device> changeEnabled(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean enabled = body.get("enabled");

        if (enabled == null) {
            throw new IllegalArgumentException(
                    "Le champ enabled est obligatoire"
            );
        }

        return ResponseEntity.ok(
                deviceService.changeEnabled(id, enabled)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}