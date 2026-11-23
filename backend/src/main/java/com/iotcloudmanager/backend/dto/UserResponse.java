package com.iotcloudmanager.backend.dto;

import com.iotcloudmanager.backend.entity.AppUser;
import com.iotcloudmanager.backend.entity.Role;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String username;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;

    public UserResponse(
            Long id,
            String username,
            Role role,
            boolean enabled,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}