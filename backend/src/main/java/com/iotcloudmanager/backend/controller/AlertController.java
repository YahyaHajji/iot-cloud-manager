
package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.dto.AlertResponse;
import com.iotcloudmanager.backend.dto.AlertThresholdRequest;
import com.iotcloudmanager.backend.dto.AlertThresholdResponse;

import com.iotcloudmanager.backend.entity.AlertThreshold;
import com.iotcloudmanager.backend.entity.Device;

import com.iotcloudmanager.backend.repository.AlertThresholdRepository;
import com.iotcloudmanager.backend.repository.DeviceRepository;
import com.iotcloudmanager.backend.repository.MeasurementTypeRepository;

import com.iotcloudmanager.backend.service.AlertService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;
    private final AlertThresholdRepository thresholdRepository;
    private final DeviceRepository deviceRepository;
    private final MeasurementTypeRepository measurementTypeRepository;

    public AlertController(
            AlertService alertService,
            AlertThresholdRepository thresholdRepository,
            DeviceRepository deviceRepository,
            MeasurementTypeRepository measurementTypeRepository
    ) {
        this.alertService = alertService;
        this.thresholdRepository = thresholdRepository;
        this.deviceRepository = deviceRepository;
        this.measurementTypeRepository = measurementTypeRepository;
    }

    // Récupérer toutes les alertes
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<AlertResponse>> getAllAlerts() {

        List<AlertResponse> result = alertService.getAllAlerts()
                .stream()
                .map(AlertResponse::from)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Récupérer les alertes d'un capteur
    @GetMapping("/device/{deviceId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AlertResponse>> getDeviceAlerts(
            @PathVariable String deviceId
    ) {

        List<AlertResponse> result = alertService.getDeviceAlerts(deviceId)
                .stream()
                .map(AlertResponse::from)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Récupérer les seuils d'un capteur
    @GetMapping("/thresholds/{deviceId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AlertThresholdResponse>> getThresholds(
            @PathVariable String deviceId
    ) {

        if (!deviceRepository.existsByDeviceId(deviceId)) {
            return ResponseEntity.notFound().build();
        }

        List<AlertThresholdResponse> result =
                thresholdRepository.findByDeviceDeviceId(deviceId)
                        .stream()
                        .map(AlertThresholdResponse::from)
                        .toList();

        return ResponseEntity.ok(result);
    }

    // Ajouter un seuil
    @PostMapping("/thresholds")
    public ResponseEntity<?> createThreshold(
            @Valid @RequestBody AlertThresholdRequest request
    ) {

        Device device = deviceRepository.findByDeviceId(request.getDeviceId())
                .orElse(null);

        if (device == null) {
            return ResponseEntity.badRequest().body("Capteur introuvable");
        }

        if (request.getMinThreshold() != null
                && request.getMaxThreshold() != null
                && request.getMinThreshold() >= request.getMaxThreshold()) {
            return ResponseEntity.badRequest()
                    .body("Le seuil minimum doit être inférieur au seuil maximum");
        }

        if (!measurementTypeRepository.existsById(request.getType())) {
            return ResponseEntity.badRequest()
                    .body("Type de mesure inconnu");
        }

        if (thresholdRepository
                .findByDeviceDeviceIdAndTypeAndEnabledTrue(
                        request.getDeviceId(), request.getType()
                ).isPresent()) {
            return ResponseEntity.badRequest()
                    .body("Un seuil actif existe déjà pour ce capteur et ce type");
        }

        AlertThreshold threshold = new AlertThreshold();
        threshold.setDevice(device);
        threshold.setType(request.getType());
        threshold.setMinThreshold(request.getMinThreshold());
        threshold.setMaxThreshold(request.getMaxThreshold());
        threshold.setEnabled(request.isEnabled());

        AlertThreshold saved = thresholdRepository.save(threshold);

        return ResponseEntity.ok(AlertThresholdResponse.from(saved));
    }

    // Modifier un seuil
    @PutMapping("/thresholds/{id}")
    @Transactional
    public ResponseEntity<?> updateThreshold(
            @PathVariable Long id,
            @Valid @RequestBody AlertThresholdRequest request
    ) {

        AlertThreshold threshold = thresholdRepository.findById(id)
                .orElse(null);

        if (threshold == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getMinThreshold() != null
                && request.getMaxThreshold() != null
                && request.getMinThreshold() >= request.getMaxThreshold()) {
            return ResponseEntity.badRequest()
                    .body("Le seuil minimum doit être inférieur au seuil maximum");
        }

        if (!measurementTypeRepository.existsById(request.getType())) {
            return ResponseEntity.badRequest()
                    .body("Type de mesure inconnu");
        }

        Device device = deviceRepository.findByDeviceId(request.getDeviceId())
                .orElse(null);

        if (device == null) {
            return ResponseEntity.badRequest().body("Capteur introuvable");
        }

        threshold.setDevice(device);
        threshold.setType(request.getType());
        threshold.setMinThreshold(request.getMinThreshold());
        threshold.setMaxThreshold(request.getMaxThreshold());
        threshold.setEnabled(request.isEnabled());

        AlertThreshold saved = thresholdRepository.save(threshold);

        return ResponseEntity.ok(AlertThresholdResponse.from(saved));
    }

    // Supprimer un seuil
    @DeleteMapping("/thresholds/{id}")
    public ResponseEntity<Void> deleteThreshold(@PathVariable Long id) {

        if (!thresholdRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        thresholdRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
