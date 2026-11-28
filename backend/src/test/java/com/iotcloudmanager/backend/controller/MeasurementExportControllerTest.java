package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.Measurement;
import com.iotcloudmanager.backend.repository.MeasurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MeasurementExportControllerTest {

    @Mock
    private MeasurementRepository measurementRepository;

    @InjectMocks
    private MeasurementController measurementController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(measurementController).build();
    }

    @Test
    void shouldExportFilteredMeasurementsAsCsv() throws Exception {
        LocalDateTime start = LocalDateTime.parse("2026-11-28T10:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-11-28T12:00:00");
        when(measurementRepository.findForCsvExport(
                "sensor-room-01", "temperature", start, end))
                .thenReturn(List.of(measurement(
                        "sensor-room-01", "temperature", 24.5, "°C",
                        LocalDateTime.parse("2026-11-28T11:00:00"))));

        mockMvc.perform(get(
                        "/api/measurements/device/sensor-room-01/export")
                        .param("type", "temperature")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("text/csv")))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"measurements.csv\""))
                .andExpect(content().string(
                        "measuredAt,deviceId,type,value,unit\r\n"
                                + "2026-11-28T11:00,\"sensor-room-01\","
                                + "\"temperature\",24.5,\"°C\"\r\n"));

        verify(measurementRepository).findForCsvExport(
                "sensor-room-01", "temperature", start, end);
    }

    @Test
    void shouldTreatBlankTypeAsNoTypeFilter() throws Exception {
        when(measurementRepository.findForCsvExport(
                "sensor-room-01", null, null, null))
                .thenReturn(List.of());

        mockMvc.perform(get(
                        "/api/measurements/device/sensor-room-01/export")
                        .param("type", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "measuredAt,deviceId,type,value,unit\r\n"));

        verify(measurementRepository).findForCsvExport(
                "sensor-room-01", null, null, null);
    }

    @Test
    void shouldRejectAnInvertedDateRange() throws Exception {
        mockMvc.perform(get(
                        "/api/measurements/device/sensor-room-01/export")
                        .param("start", "2026-11-28T12:00:00")
                        .param("end", "2026-11-28T10:00:00"))
                .andExpect(status().isBadRequest());
    }

    private static Measurement measurement(
            String deviceId,
            String type,
            double value,
            String unit,
            LocalDateTime measuredAt) {
        Device device = new Device();
        device.setDeviceId(deviceId);

        Measurement measurement = new Measurement();
        measurement.setDevice(device);
        measurement.setType(type);
        measurement.setValue(value);
        measurement.setUnit(unit);
        measurement.setMeasuredAt(measuredAt);
        return measurement;
    }
}
