
package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.dto.MeasurementResponse;
import com.iotcloudmanager.backend.repository.MeasurementRepository;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/measurements")
public class MeasurementController {

    private final MeasurementRepository measurementRepository;

    public MeasurementController(
            MeasurementRepository measurementRepository
    ) {
        this.measurementRepository = measurementRepository;
    }

    // Historique des 100 dernières mesures d'un capteur
    @GetMapping("/device/{deviceId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MeasurementResponse>> getMeasurementsByDevice(
            @PathVariable String deviceId
    ) {

        List<MeasurementResponse> result =
                measurementRepository
                        .findTop100ByDeviceDeviceIdOrderByMeasuredAtDesc(deviceId)
                        .stream()
                        .map(MeasurementResponse::from)
                        .toList();

        return ResponseEntity.ok(result);
    }

    // Historique filtré par type de mesure
    @GetMapping("/device/{deviceId}/type/{type}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MeasurementResponse>> getMeasurementsByType(
            @PathVariable String deviceId,
            @PathVariable String type
    ) {

        List<MeasurementResponse> result =
                measurementRepository
                        .findByDeviceDeviceIdAndTypeOrderByMeasuredAtDesc(
                                deviceId,
                                type
                        )
                        .stream()
                        .map(MeasurementResponse::from)
                        .toList();

        return ResponseEntity.ok(result);
    }

    // Historique filtré par période
    @GetMapping("/device/{deviceId}/period")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MeasurementResponse>> getMeasurementsByPeriod(
            @PathVariable String deviceId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {

        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "La date de début doit précéder la date de fin"
            );
        }

        List<MeasurementResponse> result =
                measurementRepository
                        .findByDeviceDeviceIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                                deviceId,
                                start,
                                end
                        )
                        .stream()
                        .map(MeasurementResponse::from)
                        .toList();

        return ResponseEntity.ok(result);
    }
}
