
package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.entity.Measurement;
import com.iotcloudmanager.backend.repository.MeasurementRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<Measurement>> getMeasurementsByDevice(
            @PathVariable String deviceId
    ) {
        return ResponseEntity.ok(
                measurementRepository
                        .findTop100ByDeviceDeviceIdOrderByMeasuredAtDesc(deviceId)
        );
    }

    @GetMapping("/device/{deviceId}/type/{type}")
    public ResponseEntity<List<Measurement>> getMeasurementsByType(
            @PathVariable String deviceId,
            @PathVariable String type
    ) {
        return ResponseEntity.ok(
                measurementRepository
                        .findByDeviceDeviceIdAndTypeOrderByMeasuredAtDesc(
                                deviceId,
                                type
                        )
        );
    }

    @GetMapping("/device/{deviceId}/period")
    public ResponseEntity<List<Measurement>> getMeasurementsByPeriod(
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

        return ResponseEntity.ok(
                measurementRepository
                        .findByDeviceDeviceIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                                deviceId,
                                start,
                                end
                        )
        );
    }
}
