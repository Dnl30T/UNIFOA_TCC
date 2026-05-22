package com.psytrack.unformulieren.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psytrack.unformulieren.adapter.in.security.SecurityConfig;
import com.psytrack.unformulieren.adapter.out.security.JwtTokenService;
import org.springframework.context.annotation.Import;
import com.psytrack.unformulieren.application.service.TeamService;
import com.psytrack.unformulieren.domain.exception.DuplicateTeamNameException;
import com.psytrack.unformulieren.domain.exception.TeamNotFoundException;
import com.psytrack.unformulieren.domain.model.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { com.psytrack.unformulieren.adapter.in.web.TeamController.class })
@Import(SecurityConfig.class)
class TeamControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean TeamService teamService;
    @MockitoBean JwtTokenService jwtTokenService;

    private final UUID managerId = UUID.randomUUID();

    private Team sampleTeam() {
        return new Team("Alpha", managerId, "ALPHA-01");
    }

    // ── POST /teams ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "MANAGER")
    void createTeam_returns201_whenManager() throws Exception {
        Team team = sampleTeam();
        given(teamService.registerTeam(any(), any())).willReturn(team);

        mockMvc.perform(post("/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Alpha", "managerId", managerId.toString())))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alpha"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createTeam_returns403_whenNotManager() throws Exception {
        mockMvc.perform(post("/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Alpha", "managerId", managerId.toString())))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createTeam_returns409_whenNameDuplicate() throws Exception {
        given(teamService.registerTeam(any(), any()))
                .willThrow(new DuplicateTeamNameException("Alpha"));

        mockMvc.perform(post("/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Alpha", "managerId", managerId.toString())))
                .with(csrf()))
                .andExpect(status().isConflict());
    }

    // ── GET /teams ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void listTeams_returns200_whenAuthenticated() throws Exception {
        given(teamService.list()).willReturn(List.of(sampleTeam()));

        mockMvc.perform(get("/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alpha"));
    }

    @Test
    void listTeams_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/teams"))
                .andExpect(status().isForbidden());
    }

    // ── GET /teams/{id} ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getTeam_returns200_whenFound() throws Exception {
        Team team = sampleTeam();
        given(teamService.get(team.getId())).willReturn(team);

        mockMvc.perform(get("/teams/" + team.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alpha"));
    }

    @Test
    @WithMockUser
    void getTeam_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        given(teamService.get(id)).willThrow(new TeamNotFoundException(id));

        mockMvc.perform(get("/teams/" + id))
                .andExpect(status().isNotFound());
    }

    // ── PUT /teams/{id} ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateTeam_returns200_whenManager() throws Exception {
        Team team = sampleTeam();
        given(teamService.update(any(), any())).willReturn(team);

        mockMvc.perform(put("/teams/" + team.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Alpha Updated", "managerId", managerId.toString())))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateTeam_returns403_whenEmployee() throws Exception {
        mockMvc.perform(put("/teams/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Alpha", "managerId", managerId.toString())))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /teams/{id} ────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteTeam_returns204_whenManager() throws Exception {
        mockMvc.perform(delete("/teams/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteTeam_returns403_whenEmployee() throws Exception {
        mockMvc.perform(delete("/teams/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteTeam_returns404_whenTeamNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TeamNotFoundException(id)).when(teamService).delete(id);

        mockMvc.perform(delete("/teams/" + id).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
