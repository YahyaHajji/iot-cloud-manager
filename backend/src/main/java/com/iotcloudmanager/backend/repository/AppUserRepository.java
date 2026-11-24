package com.iotcloudmanager.backend.repository;

import com.iotcloudmanager.backend.entity.AppUser;
import com.iotcloudmanager.backend.entity.Role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRoleAndEnabledTrue(Role role);
}