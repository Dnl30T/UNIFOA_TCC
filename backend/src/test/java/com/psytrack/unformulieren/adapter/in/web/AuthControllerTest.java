package com.psytrack.unformulieren.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.psytrack.unformulieren.adapter.in.security.SecurityConfig;
import com.psytrack.unformulieren.adapter.out.security.JwtTokenService;
import org.springframework.context.annotation.Import;
import com.psytrack.unformulieren.application.service.AuthService;
import com.psytrack.unformulieren.domain.enums.UserRole;
import com.psytrack.unformulieren.domain.exception.TeamNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { com.psytrack.unformulieren.adapter.in.web.AuthController.class })
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean AuthService authService;
    @MockitoBean JwtTokenService jwtTokenService;

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Test
    void login_returns200_withToken() throws Exception {
        given(authService.login("alice@example.com", "secret")).willReturn("jwt-token");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "alice@example.com", "password", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_returns401_whenBadCredentials() throws Exception {
        given(authService.login(anyString(), anyString()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "alice@example.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /auth/register/employee ──────────────────────────────────────────

    @Test
    void registerEmployee_returns201_onSuccess() throws Exception {
        given(authService.registerEmployee("bob", "pass", "bob@example.com", "ALPHA-01", null, null, null, null))
                .willReturn("employee-token");

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "bob", "password", "pass",
                "email", "bob@example.com", "teamCode", "ALPHA-01"));

        mockMvc.perform(post("/auth/register/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("employee-token"));
    }

    @Test
    void registerEmployee_returns400_whenTeamNotFound() throws Exception {
        given(authService.registerEmployee(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .willThrow(new TeamNotFoundException("NOPE"));

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "bob", "password", "pass",
                "email", "bob@example.com", "teamCode", "NOPE"));

        mockMvc.perform(post("/auth/register/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNotFound());
    }

    // ── POST /auth/register/staff ─────────────────────────────────────────────

    @Test
    void registerStaff_returns201_onSuccess() throws Exception {
        given(authService.registerStaff("carol", "pass", "carol@example.com", null, null, null, null))
                .willReturn("staff-token");

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "carol", "password", "pass", "email", "carol@example.com"));

        mockMvc.perform(post("/auth/register/staff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("staff-token"));
    }

    @Test
    void registerStaff_returns400_whenUsernameBlank() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "", "password", "pass", "email", "carol@example.com"));

        mockMvc.perform(post("/auth/register/staff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/claim-role ─────────────────────────────────────────────────

    @Test
    void claimRole_returns200_forPendingUser() throws Exception {
        given(authService.claimRole("dave", UserRole.MANAGER)).willReturn("manager-token");

        var pendingAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "dave", null,
                List.of(new SimpleGrantedAuthority("ROLE_PENDING")));

        mockMvc.perform(post("/auth/claim-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "MANAGER")))
                .with(authentication(pendingAuth))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("manager-token"));
    }

    @Test
    void claimRole_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/auth/claim-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "MANAGER"))))
                .andExpect(status().isForbidden());
    }
}
