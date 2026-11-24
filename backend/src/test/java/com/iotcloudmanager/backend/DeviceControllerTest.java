
package com.iotcloudmanager.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iotcloudmanager.backend.controller.DeviceController;
import com.iotcloudmanager.backend.dto.DeviceRequest;
import com.iotcloudmanager.backend.dto.DeviceResponse;
import com.iotcloudmanager.backend.entity.Device;
import com.iotcloudmanager.backend.entity.DeviceStatus;
import com.iotcloudmanager.backend.service.DeviceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private DeviceController deviceController;

    private Device device;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deviceController).build();
        objectMapper = new ObjectMapper();

        device = new Device();
        device.setDeviceId("sensor-001");
        device.setName("Capteur température");
        device.setIntervalSec(60);
        device.setEnabled(true);
        device.setStatus(DeviceStatus.ONLINE);
        device.setLocation("Salle serveur");
        device.setDescription("Capteur de température");
    }

    @Test
    void shouldGetAllDevices() throws Exception {

        DeviceResponse response =
                DeviceResponse.from(device, Collections.emptyList());

        when(deviceService.getAllDevices())
                .thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("sensor-001"))
                .andExpect(jsonPath("$[0].location").value("Salle serveur"))
                .andExpect(jsonPath("$[0].description")
                        .value("Capteur de température"))
                .andExpect(jsonPath("$[0].lastMeasurements").isArray())
                .andExpect(jsonPath("$[0].lastMeasurements").isEmpty());
    }

    @Test
    void shouldGetDeviceById() throws Exception {
        when(deviceService.getDeviceById(1L)).thenReturn(device);

        mockMvc.perform(get("/api/devices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("sensor-001"));
    }

    @Test
    void shouldCreateDevice() throws Exception {
        when(deviceService.createDevice(any(DeviceRequest.class)))
                .thenReturn(device);

        DeviceRequest request = new DeviceRequest();
        request.setDeviceId("sensor-001");
        request.setName("Capteur température");
        request.setIntervalSec(60);
        request.setLocation("Salle serveur");
        request.setDescription("Capteur de température");

        mockMvc.perform(post("/api/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value("sensor-001"))
                .andExpect(jsonPath("$.location").value("Salle serveur"))
                .andExpect(jsonPath("$.description")
                        .value("Capteur de température"));
    }

    @Test
    void shouldUpdateDevice() throws Exception {
        when(deviceService.updateDevice(eq(1L), any(DeviceRequest.class)))
                .thenReturn(device);

        DeviceRequest request = new DeviceRequest();
        request.setDeviceId("sensor-001");
        request.setName("Capteur modifié");
        request.setIntervalSec(60);
        request.setLocation("Salle serveur");
        request.setDescription("Capteur de température");

        mockMvc.perform(put("/api/devices/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("sensor-001"));
    }

    @Test
    void shouldChangeEnabled() throws Exception {
        when(deviceService.changeEnabled(1L, false))
                .thenReturn(device);

        mockMvc.perform(patch("/api/devices/1/enabled")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteDevice() throws Exception {
        doNothing().when(deviceService).deleteDevice(1L);

        mockMvc.perform(delete("/api/devices/1"))
                .andExpect(status().isNoContent());

        verify(deviceService).deleteDevice(1L);
    }
}
