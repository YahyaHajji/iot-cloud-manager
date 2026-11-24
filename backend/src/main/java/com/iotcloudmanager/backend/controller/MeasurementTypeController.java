
package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.entity.MeasurementType;
import com.iotcloudmanager.backend.repository.MeasurementTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/measurement-types")
public class MeasurementTypeController {

    private final MeasurementTypeRepository repository;

    public MeasurementTypeController(
            MeasurementTypeRepository repository
    ) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<MeasurementType>> getAllMeasurementTypes() {
        return ResponseEntity.ok(repository.findByEnabledTrue());
    }
}
