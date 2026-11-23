package com.iotcloudmanager.backend.controller;

import com.iotcloudmanager.backend.dto.ChangeUserEnabledRequest;
import com.iotcloudmanager.backend.dto.ChangeUserRoleRequest;
import com.iotcloudmanager.backend.dto.CreateUserRequest;
import com.iotcloudmanager.backend.dto.UserResponse;
import com.iotcloudmanager.backend.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Créer un utilisateur
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse created = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // Lister tous les utilisateurs
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Consulter un utilisateur par son ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // Activer ou désactiver un utilisateur
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<UserResponse> changeEnabled(
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserEnabledRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(
                userService.changeEnabled(
                        id,
                        request.getEnabled(),
                        principal.getUsername()
                )
        );
    }

    // Modifier le rôle d'un utilisateur
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserRoleRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(
                userService.changeRole(
                        id,
                        request.getRole(),
                        principal.getUsername()
                )
        );
    }

    // Supprimer un utilisateur
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        userService.deleteUser(id, principal.getUsername());

        return ResponseEntity.noContent().build();
    }
}
