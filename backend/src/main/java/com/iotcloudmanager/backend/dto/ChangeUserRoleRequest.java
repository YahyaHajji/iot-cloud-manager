package com.iotcloudmanager.backend.dto;

import com.iotcloudmanager.backend.entity.Role;
import jakarta.validation.constraints.NotNull;

public class ChangeUserRoleRequest {

    @NotNull
    private Role role;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}