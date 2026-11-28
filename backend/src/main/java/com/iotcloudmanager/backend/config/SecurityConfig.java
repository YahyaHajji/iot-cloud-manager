package com.iotcloudmanager.backend.config;

import com.iotcloudmanager.backend.security.JwtAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter
    ) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(Customizer.withDefaults())

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            .exceptionHandling(exception -> exception

                .authenticationEntryPoint((request, response, ex) -> {

                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    response.getWriter().write(
                            "{\"status\":401,\"message\":\"Authentification requise ou token invalide\"}"
                    );
                })

                .accessDeniedHandler((request, response, ex) -> {

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    response.getWriter().write(
                            "{\"status\":403,\"message\":\"Accès refusé : permissions insuffisantes\"}"
                    );
                })
            )

            .authorizeHttpRequests(auth -> auth

                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Connexion publique
                .requestMatchers("/api/auth/login").permitAll()

                // Santé publique
                .requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus"
                ).permitAll()

                // Gestion des utilisateurs : ADMIN uniquement
                .requestMatchers(
                        "/api/users",
                        "/api/users/**"
                ).hasRole("ADMIN")

                // Commandes des appareils : ADMIN uniquement
                .requestMatchers(
                        "/api/devices/*/commands",
                        "/api/devices/*/commands/**"
                ).hasRole("ADMIN")

                // Gestion des seuils : ADMIN uniquement
                .requestMatchers(
                        "/api/alerts/thresholds",
                        "/api/alerts/thresholds/**"
                ).hasRole("ADMIN")

                // Consultation : ADMIN et USER
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/devices/**",
                        "/api/measurements/**",
                        "/api/alerts/**",
                        "/api/measurement-types/**",
                        "/api/auth/me"
                ).hasAnyRole("ADMIN", "USER")

                // Supervision
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Toutes les autres routes API : ADMIN
                .requestMatchers("/api/**").hasRole("ADMIN")

                .anyRequest().denyAll()
            )

            .addFilterBefore(
                    jwtFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}