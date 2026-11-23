
package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.dto.CreateUserRequest;
import com.iotcloudmanager.backend.dto.UserResponse;

import com.iotcloudmanager.backend.entity.AppUser;
import com.iotcloudmanager.backend.entity.Role;

import com.iotcloudmanager.backend.repository.AppUserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Créer un utilisateur
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        String username = request.getUsername()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ce nom d'utilisateur existe déjà"
            );
        }

        String password = request.getPassword();

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le mot de passe dépasse 72 octets UTF-8"
            );
        }

        Role role = request.getRole() == null
                ? Role.USER
                : request.getRole();

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);

        AppUser saved = userRepository.save(user);

        return UserResponse.from(saved);
    }

    // Récupérer tous les utilisateurs
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    // Récupérer un utilisateur par son ID
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        AppUser user = findUser(id);

        return UserResponse.from(user);
    }

    // Récupérer un utilisateur par son username
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {

        AppUser user = findUserByUsername(username);

        return UserResponse.from(user);
    }

    // Activer ou désactiver un utilisateur
    @Transactional
    public UserResponse changeEnabled(
            Long id,
            boolean enabled,
            String currentUsername
    ) {

        AppUser user = findUser(id);

        if (user.getUsername().equals(currentUsername) && !enabled) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas désactiver votre propre compte"
            );
        }

        if (!enabled) {
            protectLastAdmin(user);
        }

        user.setEnabled(enabled);

        return UserResponse.from(userRepository.save(user));
    }

    // Modifier le rôle d'un utilisateur
    @Transactional
    public UserResponse changeRole(
            Long id,
            Role newRole,
            String currentUsername
    ) {

        AppUser user = findUser(id);

        if (user.getUsername().equals(currentUsername)
                && newRole != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas supprimer votre propre rôle ADMIN"
            );
        }

        if (user.getRole() == Role.ADMIN
                && newRole != Role.ADMIN) {
            protectLastAdmin(user);
        }

        user.setRole(newRole);

        return UserResponse.from(userRepository.save(user));
    }

    // Supprimer un utilisateur
    @Transactional
    public void deleteUser(Long id, String currentUsername) {

        AppUser user = findUser(id);

        if (user.getUsername().equals(currentUsername)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas supprimer votre propre compte"
            );
        }

        protectLastAdmin(user);

        userRepository.delete(user);
    }

    // Rechercher un utilisateur par ID
    private AppUser findUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Utilisateur introuvable"
                        ));
    }

    // Rechercher un utilisateur par username
    private AppUser findUserByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Utilisateur introuvable"
                        ));
    }

    // Protéger le dernier administrateur actif
    private void protectLastAdmin(AppUser user) {

        if (user.getRole() == Role.ADMIN
                && user.isEnabled()
                && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Impossible de supprimer ou désactiver le dernier administrateur actif"
            );
        }
    }
}
