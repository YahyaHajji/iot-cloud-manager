package com.iotcloudmanager.backend.service;

import com.iotcloudmanager.backend.dto.LoginRequest;
import com.iotcloudmanager.backend.dto.LoginResponse;
import com.iotcloudmanager.backend.entity.AppUser;
import com.iotcloudmanager.backend.entity.Role;
import com.iotcloudmanager.backend.repository.AppUserRepository;
import com.iotcloudmanager.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static LoginRequest request(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private static AppUser user(String username, Role role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    @Test
    void shouldReturnATokenForValidCredentials() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user("admin", Role.ADMIN)));
        when(jwtService.generateToken("admin", "ADMIN"))
                .thenReturn("jeton-jwt");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response =
                authService.login(request("admin", "motdepasse"));

        assertEquals("jeton-jwt", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void shouldReturnTheRoleOfARegularUser() {
        when(userRepository.findByUsername("lecteur"))
                .thenReturn(Optional.of(user("lecteur", Role.USER)));
        when(jwtService.generateToken("lecteur", "USER"))
                .thenReturn("jeton-lecture");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response =
                authService.login(request("lecteur", "motdepasse"));

        assertEquals("USER", response.getRole());
    }

    @Test
    void shouldNormaliseTheUsernameBeforeAuthenticating() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user("admin", Role.ADMIN)));
        when(jwtService.generateToken("admin", "ADMIN"))
                .thenReturn("jeton-jwt");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        authService.login(request("  Admin ", "motdepasse"));

        ArgumentCaptor<Authentication> captor =
                ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());

        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) captor.getValue();
        assertEquals("admin", token.getPrincipal());
        assertEquals("motdepasse", token.getCredentials());
    }

    @Test
    void shouldRefuseAWrongPasswordWithoutIssuingAToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Identifiants invalides"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request("admin", "faux")));

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void shouldRefuseADisabledUser() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Compte désactivé"));

        assertThrows(DisabledException.class,
                () -> authService.login(request("admin", "motdepasse")));

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void shouldFailWhenTheAuthenticatedUserIsMissingFromTheDatabase() {
        when(userRepository.findByUsername("fantome"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.login(request("fantome", "motdepasse")));

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }
}
