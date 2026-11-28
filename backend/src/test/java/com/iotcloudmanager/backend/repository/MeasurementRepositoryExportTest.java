package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.Measurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:measurement-export;NON_KEYWORDS=VALUE"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MeasurementRepositoryExportTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    private Device device;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setDeviceId("sensor-export-01");
        device.setName("Export test sensor");
        device = deviceRepository.saveAndFlush(device);

        saveMeasurement("temperature", 20.0, "°C", "2026-11-28T10:00:00");
        saveMeasurement("temperature", 24.5, "°C", "2026-11-28T11:00:00");
        saveMeasurement("humidity", 50.0, "%", "2026-11-28T12:00:00");
        measurementRepository.flush();
    }

    @Test
    void shouldApplyOptionalExportFiltersAndOrderRowsChronologically() {
        List<Measurement> results = measurementRepository.findForCsvExport(
                "sensor-export-01",
                "temperature",
                LocalDateTime.parse("2026-11-28T10:30:00"),
                LocalDateTime.parse("2026-11-28T11:30:00"));

        assertEquals(1, results.size());
        assertEquals(24.5, results.get(0).getValue());
        assertEquals("temperature", results.get(0).getType());
    }

    @Test
    void shouldExportAllDeviceMeasurementsWhenFiltersAreAbsent() {
        List<Measurement> results = measurementRepository.findForCsvExport(
                "sensor-export-01", null, null, null);

        assertEquals(3, results.size());
        assertEquals(
                LocalDateTime.parse("2026-11-28T10:00:00"),
                results.get(0).getMeasuredAt());
        assertEquals(
                LocalDateTime.parse("2026-11-28T12:00:00"),
                results.get(2).getMeasuredAt());
    }

    private void saveMeasurement(
            String type, double value, String unit, String measuredAt) {
        Measurement measurement = new Measurement();
        measurement.setDevice(device);
        measurement.setMessageId("message-" + type + "-" + measuredAt);
        measurement.setType(type);
        measurement.setValue(value);
        measurement.setUnit(unit);
        measurement.setMeasuredAt(LocalDateTime.parse(measuredAt));
        measurement.setReceivedAt(LocalDateTime.parse(measuredAt));
        measurementRepository.save(measurement);
    }
}
