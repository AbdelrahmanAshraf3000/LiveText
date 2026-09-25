package com.example.backend.service;

import com.example.backend.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-test-secret-key-test-secret-key-test-secret-key-test-secret-key",
                Duration.ofMinutes(15),
                Duration.ofDays(7));
        jwtService = new JwtService(properties);
    }

    @Test
    void generateAndParseAccessToken() {
        JwtService.TokenPair tokens = jwtService.generate(1L, "alice");

        assertThat(jwtService.isValid(tokens.accessToken())).isTrue();
        assertThat(jwtService.getUsername(tokens.accessToken())).isEqualTo("alice");
        assertThat(jwtService.getUserId(tokens.accessToken())).isEqualTo(1L);
    }

    @Test
    void refreshTokenIsMarkedAsRefresh() {
        JwtService.TokenPair tokens = jwtService.generate(1L, "alice");

        assertThat(jwtService.isRefreshToken(tokens.refreshToken())).isTrue();
        assertThat(jwtService.isRefreshToken(tokens.accessToken())).isFalse();
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isRefreshToken("not-a-jwt")).isFalse();
    }
}