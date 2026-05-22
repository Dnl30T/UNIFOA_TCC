package com.psytrack.unformulieren.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psytrack.unformulieren.adapter.in.security.SecurityConfig;
import com.psytrack.unformulieren.adapter.out.security.JwtTokenService;
import org.springframework.context.annotation.Import;
import com.psytrack.unformulieren.application.service.FormService;
import com.psytrack.unformulieren.domain.enums.Status;
import com.psytrack.unformulieren.domain.exception.FormNotFoundException;
import com.psytrack.unformulieren.domain.model.Form;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { com.psytrack.unformulieren.adapter.in.web.FormController.class })
@Import(SecurityConfig.class)
class FormControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean FormService formService;
    @MockitoBean JwtTokenService jwtTokenService;

    private Form sampleForm() {
        return new Form("Health Check", "Monthly survey", Status.ACTIVE);
    }

    // ── POST /forms ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void createForm_returns201_whenCounselor() throws Exception {
        Form form = sampleForm();
        given(formService.create(any(), any(), any(), any())).willReturn(form);

        mockMvc.perform(post("/forms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("title", "Health Check", "description", "Monthly survey", "status", "ACTIVE")))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Health Check"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createForm_returns403_whenManager() throws Exception {
        mockMvc.perform(post("/forms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("title", "Form", "description", "", "status", "ACTIVE")))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void createForm_returns400_whenTitleBlank() throws Exception {
        mockMvc.perform(post("/forms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("title", "", "description", "", "status", "ACTIVE")))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ── GET /forms ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void listForms_returns200_whenAuthenticated() throws Exception {
        given(formService.list()).willReturn(List.of(sampleForm()));

        mockMvc.perform(get("/forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Health Check"));
    }

    @Test
    @WithMockUser
    void listForms_withStatusFilter_delegatesToFindByStatus() throws Exception {
        given(formService.findByStatus(Status.ACTIVE)).willReturn(List.of(sampleForm()));

        mockMvc.perform(get("/forms").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void listForms_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/forms"))
                .andExpect(status().isForbidden());
    }

    // ── GET /forms/{id} ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getForm_returns200_whenFound() throws Exception {
        Form form = sampleForm();
        given(formService.get(form.getId())).willReturn(form);

        mockMvc.perform(get("/forms/" + form.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Health Check"));
    }

    @Test
    @WithMockUser
    void getForm_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        given(formService.get(id)).willThrow(new FormNotFoundException(id));

        mockMvc.perform(get("/forms/" + id))
                .andExpect(status().isNotFound());
    }

    // ── GET /forms/by-title ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findByTitle_returns200_whenFormExists() throws Exception {
        Form form = sampleForm();
        given(formService.findByTitle("Health Check")).willReturn(Optional.of(form));

        mockMvc.perform(get("/forms/by-title").param("title", "Health Check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Health Check"));
    }

    // ── PUT /forms/{id} ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void updateForm_returns200_whenCounselor() throws Exception {
        Form form = sampleForm();
        given(formService.update(any(), any(), any(), any(), any())).willReturn(form);

        mockMvc.perform(put("/forms/" + form.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("title", "Health Check", "description", "Updated", "status", "ACTIVE")))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateForm_returns403_whenEmployee() throws Exception {
        mockMvc.perform(put("/forms/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("title", "Form", "description", "", "status", "ACTIVE")))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /forms/{id} ────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void deleteForm_returns204_whenCounselor() throws Exception {
        mockMvc.perform(delete("/forms/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteForm_returns403_whenManager() throws Exception {
        mockMvc.perform(delete("/forms/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void deleteForm_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new FormNotFoundException(id)).when(formService).delete(id);

        mockMvc.perform(delete("/forms/" + id).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
