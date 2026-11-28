package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.dto.CreateDeviceCommandRequest;
import com.iotcloudmanager.backend.dto.DeviceCommandResponse;
import com.iotcloudmanager.backend.service.DeviceCommandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceCommandController {

    private final DeviceCommandService commandService;

    public DeviceCommandController(DeviceCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/{id}/commands")
    public ResponseEntity<DeviceCommandResponse> createCommand(
            @PathVariable Long id,
            @Valid @RequestBody CreateDeviceCommandRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commandService.createCommand(id, request));
    }

    @GetMapping("/{id}/commands")
    public ResponseEntity<List<DeviceCommandResponse>> getCommandHistory(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                commandService.getCommandHistoryByNumericId(id)
        );
    }
}
