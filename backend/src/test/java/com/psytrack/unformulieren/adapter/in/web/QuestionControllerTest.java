package com.psytrack.unformulieren.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psytrack.unformulieren.adapter.in.security.SecurityConfig;
import com.psytrack.unformulieren.adapter.out.security.JwtTokenService;
import org.springframework.context.annotation.Import;
import com.psytrack.unformulieren.application.service.QuestionService;
import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.exception.QuestionNotFoundException;
import com.psytrack.unformulieren.domain.model.Question;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { com.psytrack.unformulieren.adapter.in.web.QuestionController.class })
@Import(SecurityConfig.class)
class QuestionControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean QuestionService questionService;
    @MockitoBean JwtTokenService jwtTokenService;

    private final UUID formId = UUID.randomUUID();

    private Question sampleQuestion() {
        return new Question(formId, "How do you feel today?", QuestionType.SCALE, true, Map.of(), 0);
    }

    // ── POST /questions ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void createQuestion_returns201_whenCounselor() throws Exception {
        Question question = sampleQuestion();
        given(questionService.create(any(), any(), any(), anyBoolean(), any(), anyInt())).willReturn(question);

        mockMvc.perform(post("/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "formId", formId.toString(),
                        "text", "How do you feel today?",
                        "type", "SCALE",
                        "required", true,
                        "config", Map.of(),
                        "order", 0)))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("How do you feel today?"))
                .andExpect(jsonPath("$.type").value("SCALE"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createQuestion_returns403_whenEmployee() throws Exception {
        mockMvc.perform(post("/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "formId", formId.toString(),
                        "text", "Question?",
                        "type", "TEXT",
                        "required", false,
                        "config", Map.of(),
                        "order", 0)))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void createQuestion_returns400_whenTextBlank() throws Exception {
        mockMvc.perform(post("/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "formId", formId.toString(),
                        "text", "",
                        "type", "TEXT",
                        "required", false,
                        "config", Map.of(),
                        "order", 0)))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ── GET /questions ────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void listQuestions_returns200_whenAuthenticated() throws Exception {
        given(questionService.list()).willReturn(List.of(sampleQuestion()));

        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("How do you feel today?"));
    }

    @Test
    @WithMockUser
    void listQuestions_withTypeFilter_delegatesToFindByType() throws Exception {
        given(questionService.findByType(QuestionType.SCALE)).willReturn(List.of(sampleQuestion()));

        mockMvc.perform(get("/questions").param("type", "SCALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SCALE"));
    }

    @Test
    void listQuestions_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isForbidden());
    }

    // ── GET /questions/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getQuestion_returns200_whenFound() throws Exception {
        Question question = sampleQuestion();
        given(questionService.get(question.getId())).willReturn(question);

        mockMvc.perform(get("/questions/" + question.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("How do you feel today?"));
    }

    @Test
    @WithMockUser
    void getQuestion_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        given(questionService.get(id)).willThrow(new QuestionNotFoundException(id));

        mockMvc.perform(get("/questions/" + id))
                .andExpect(status().isNotFound());
    }

    // ── PUT /questions/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void updateQuestion_returns200_whenCounselor() throws Exception {
        Question question = sampleQuestion();
        given(questionService.update(any(), any(), any(), any(), anyBoolean(), any(), anyInt())).willReturn(question);

        mockMvc.perform(put("/questions/" + question.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "formId", formId.toString(),
                        "text", "Updated question?",
                        "type", "SCALE",
                        "required", true,
                        "config", Map.of(),
                        "order", 1)))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateQuestion_returns403_whenManager() throws Exception {
        mockMvc.perform(put("/questions/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "formId", formId.toString(),
                        "text", "Question?",
                        "type", "TEXT",
                        "required", false,
                        "config", Map.of(),
                        "order", 0)))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /questions/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void deleteQuestion_returns204_whenCounselor() throws Exception {
        mockMvc.perform(delete("/questions/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteQuestion_returns403_whenEmployee() throws Exception {
        mockMvc.perform(delete("/questions/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COUNSELOR")
    void deleteQuestion_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new QuestionNotFoundException(id)).when(questionService).delete(id);

        mockMvc.perform(delete("/questions/" + id).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
