package com.psytrack.unformulieren.adapter.out.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    // Must be at least 32 chars for HMAC-SHA256
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonNullJwt() {
        String token = jwtTokenService.generateToken("alice", "EMPLOYEE");
        assertThat(token).isNotBlank();
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtTokenService.generateToken("bob", "MANAGER");
        assertThat(jwtTokenService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalseForGarbageToken() {
        assertThat(jwtTokenService.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_returnsFalseForEmptyString() {
        assertThat(jwtTokenService.validateToken("")).isFalse();
    }

    @Test
    void extractUsername_returnsCorrectSubject() {
        String token = jwtTokenService.generateToken("carol", "COUNSELOR");
        assertThat(jwtTokenService.extractUsername(token)).isEqualTo("carol");
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtTokenService.generateToken("dave", "ADMIN");
        assertThat(jwtTokenService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void expiredToken_isInvalid() {
        // 1 ms expiration — token is expired immediately
        JwtTokenService shortLived = new JwtTokenService(SECRET, 1L);
        String token = shortLived.generateToken("eve", "PENDING");

        // Yield the current thread briefly to ensure the token has expired
        try { Thread.sleep(10); } catch (InterruptedException ignored) { }

        assertThat(shortLived.validateToken(token)).isFalse();
    }
}
