package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.dto.LoginRequest;
import com.iotcloudmanager.backend.dto.LoginResponse;

import com.iotcloudmanager.backend.entity.AppUser;
import com.iotcloudmanager.backend.repository.AppUserRepository;
import com.iotcloudmanager.backend.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            AppUserRepository userRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        String username = request.getUsername().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        request.getPassword()
                )
        );

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Utilisateur introuvable"));

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole().name()
        );

        return new LoginResponse(
                token,
                jwtService.getExpirationMs() / 1000,
                user.getUsername(),
                user.getRole().name()
        );
    }
}