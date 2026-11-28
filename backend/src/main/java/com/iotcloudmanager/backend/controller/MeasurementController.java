
package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.dto.MeasurementResponse;
import com.iotcloudmanager.backend.entity.Measurement;
import com.iotcloudmanager.backend.repository.MeasurementRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/measurements")
@SecurityRequirement(name = "bearerAuth")
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

    @GetMapping("/device/{deviceId}/export")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Exporter les mesures d'un capteur en CSV",
            description = "Les bornes start et end sont inclusives et utilisent "
                    + "le format ISO 8601 local."
    )
    public ResponseEntity<String> exportMeasurements(
            @PathVariable String deviceId,
            @Parameter(description = "Type de mesure à filtrer")
            @RequestParam(required = false) String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Date de début inclusive (ISO 8601)")
            LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Date de fin inclusive (ISO 8601)")
            LocalDateTime end
    ) {
        if (start != null && end != null && start.isAfter(end)) {
            return ResponseEntity.badRequest().build();
        }

        String filterType = type == null || type.isBlank() ? null : type;
        List<Measurement> measurements = measurementRepository.findForCsvExport(
                deviceId, filterType, start, end);

        StringBuilder csv = new StringBuilder(
                "measuredAt,deviceId,type,value,unit\r\n");
        for (Measurement measurement : measurements) {
            csv.append(measurement.getMeasuredAt())
                    .append(',')
                    .append(csvText(measurement.getDevice().getDeviceId()))
                    .append(',')
                    .append(csvText(measurement.getType()))
                    .append(',')
                    .append(measurement.getValue())
                    .append(',')
                    .append(csvText(measurement.getUnit()))
                    .append("\r\n");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"measurements.csv\"")
                .body(csv.toString());
    }

    private static String csvText(String value) {
        String safeValue = value;
        if (!safeValue.isEmpty()
                && "=+-@\t\r".indexOf(safeValue.charAt(0)) >= 0) {
            safeValue = "'" + safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
