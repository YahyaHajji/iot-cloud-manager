package com.iotcloudmanager.backend.config;

import com.iotcloudmanager.backend.entity.MeasurementType;
import com.iotcloudmanager.backend.repository.MeasurementTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeasurementTypeInitializer {

    @Bean
    CommandLineRunner initializeMeasurementTypes(
            MeasurementTypeRepository repository
    ) {
        return args -> {

            addType(repository, "temperature", "°C", -40.0, 85.0);
            addType(repository, "humidity", "%", 0.0, 100.0);
            addType(repository, "pressure", "hPa", 300.0, 1100.0);
            addType(repository, "co2", "ppm", 0.0, 10000.0);
            addType(repository, "water_volume", "L", 0.0, null);
            addType(repository, "presence", "bool", 0.0, 1.0);

        };
    }

    private void addType(
            MeasurementTypeRepository repository,
            String type,
            String unit,
            Double min,
            Double max
    ) {

        if (!repository.existsById(type)) {

            MeasurementType measurementType = new MeasurementType();

            measurementType.setType(type);
            measurementType.setUnit(unit);
            measurementType.setMinValue(min);
            measurementType.setMaxValue(max);
            measurementType.setEnabled(true);

            repository.save(measurementType);
        }
    }
}