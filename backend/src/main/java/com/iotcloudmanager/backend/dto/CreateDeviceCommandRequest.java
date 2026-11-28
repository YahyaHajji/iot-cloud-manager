package com.iotcloudmanager.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateDeviceCommandRequest {

    @NotBlank
    private String type;

    private Map<String, Object> params = Map.of();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? Map.of() : params;
    }
}
