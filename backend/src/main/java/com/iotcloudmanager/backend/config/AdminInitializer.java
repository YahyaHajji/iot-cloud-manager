package com.iotcloudmanager.backend.config;

import com.iotcloudmanager.backend.entity.AppUser;
import com.iotcloudmanager.backend.entity.Role;
import com.iotcloudmanager.backend.repository.AppUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:}") String adminUsername,
            @Value("${app.admin.password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {

        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_USERNAME et ADMIN_PASSWORD sont obligatoires"
            );
        }

        if (adminPassword.length() < 12
                || adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Le mot de passe ADMIN doit contenir au moins 12 caractères et au maximum 72 octets UTF-8"
            );
        }

        String username = adminUsername.trim().toLowerCase(Locale.ROOT);

        AppUser existing = userRepository.findByUsername(username).orElse(null);

        if (existing != null) {

            if (existing.getRole() != Role.ADMIN || !existing.isEnabled()) {
                existing.setRole(Role.ADMIN);
                existing.setEnabled(true);
                userRepository.save(existing);
            }

            return;
        }

        AppUser admin = new AppUser();

        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);

        System.out.println("Compte administrateur initial créé : " + username);
    }
}