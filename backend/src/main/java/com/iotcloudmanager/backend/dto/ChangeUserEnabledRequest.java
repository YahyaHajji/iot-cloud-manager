package com.iotcloudmanager.backend.dto;

import jakarta.validation.constraints.NotNull;

public class ChangeUserEnabledRequest {

    @NotNull
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}