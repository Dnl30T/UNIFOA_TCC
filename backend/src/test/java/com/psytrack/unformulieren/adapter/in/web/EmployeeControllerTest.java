package com.psytrack.unformulieren.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psytrack.unformulieren.adapter.in.security.SecurityConfig;
import com.psytrack.unformulieren.adapter.out.security.JwtTokenService;
import org.springframework.context.annotation.Import;
import com.psytrack.unformulieren.application.service.EmployeeService;
import com.psytrack.unformulieren.domain.enums.EmployeeStatus;
import com.psytrack.unformulieren.domain.exception.EmployeeNotFoundException;
import com.psytrack.unformulieren.domain.model.Employee;
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

@WebMvcTest(controllers = { com.psytrack.unformulieren.adapter.in.web.EmployeeController.class })
@Import(SecurityConfig.class)
class EmployeeControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean EmployeeService employeeService;
    @MockitoBean JwtTokenService jwtTokenService;

    private final UUID appUserId = UUID.randomUUID();
    private final UUID teamId    = UUID.randomUUID();

    private Employee sampleEmployee() {
        return Employee.hire("Alice", appUserId, teamId);
    }

    // ── POST /employees ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEmployee_returns201_whenAdmin() throws Exception {
        Employee employee = sampleEmployee();
        given(employeeService.hireEmployee(any(), any(), any())).willReturn(employee);

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Alice",
                        "appUserId", appUserId.toString(),
                        "teamId", teamId.toString())))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createEmployee_returns403_whenEmployee() throws Exception {
        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Bob",
                        "appUserId", appUserId.toString(),
                        "teamId", teamId.toString())))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEmployee_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "",
                        "appUserId", appUserId.toString(),
                        "teamId", teamId.toString())))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ── GET /employees ────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void listEmployees_returns200_whenAuthenticated() throws Exception {
        given(employeeService.list()).willReturn(List.of(sampleEmployee()));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    void listEmployees_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isForbidden());
    }

    // ── GET /employees/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getEmployee_returns200_whenFound() throws Exception {
        Employee employee = sampleEmployee();
        given(employeeService.get(employee.getId())).willReturn(employee);

        mockMvc.perform(get("/employees/" + employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @WithMockUser
    void getEmployee_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        given(employeeService.get(id)).willThrow(new EmployeeNotFoundException(id));

        mockMvc.perform(get("/employees/" + id))
                .andExpect(status().isNotFound());
    }

    // ── PUT /employees/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateEmployee_returns200_whenAdmin() throws Exception {
        Employee employee = sampleEmployee();
        given(employeeService.update(any(), any(), any(), any())).willReturn(employee);

        mockMvc.perform(put("/employees/" + employee.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Alice Updated",
                        "appUserId", appUserId.toString(),
                        "teamId", teamId.toString(),
                        "status", "ACTIVE")))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmployee_returns403_whenEmployee() throws Exception {
        mockMvc.perform(put("/employees/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Alice",
                        "appUserId", appUserId.toString(),
                        "teamId", teamId.toString())))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /employees/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteEmployee_returns204_whenAdmin() throws Exception {
        mockMvc.perform(delete("/employees/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteEmployee_returns403_whenEmployee() throws Exception {
        mockMvc.perform(delete("/employees/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteEmployee_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EmployeeNotFoundException(id)).when(employeeService).delete(id);

        mockMvc.perform(delete("/employees/" + id).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
