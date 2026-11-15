package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.model.Device;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    // Liste en mémoire temporaire (en attendant la base de données de la semaine prochaine)
    private final List<Device> devices = new ArrayList<>();

    public DeviceController() {
        // Ajout d'un appareil par défaut pour les tests
        devices.add(new Device("sensor-room-01", "Capteur Salle 1", true, "temperature", "°C", -10.0, 50.0));
    }

    @GetMapping
    public List<Device> getAllDevices() {
        return devices;
    }

    @PostMapping
    public ResponseEntity<Device> createDevice(@RequestBody Device newDevice) {
        devices.add(newDevice);
        return ResponseEntity.ok(newDevice);
    }
}