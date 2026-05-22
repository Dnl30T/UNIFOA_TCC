package com.psytrack.unformulieren.application.service;

import com.psytrack.unformulieren.application.port.out.EmployeeRepositoryPort;
import com.psytrack.unformulieren.domain.enums.EmployeeStatus;
import com.psytrack.unformulieren.domain.exception.EmployeeNotFoundException;
import com.psytrack.unformulieren.domain.model.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks private EmployeeService employeeService;

    private final UUID appUserId = UUID.randomUUID();
    private final UUID teamId    = UUID.randomUUID();

    private Employee sampleEmployee() {
        return Employee.hire("Alice", appUserId, teamId);
    }

    // ── hireEmployee ─────────────────────────────────────────────────────────

    @Test
    void hireEmployee_savesAndReturnsEmployee() {
        Employee saved = sampleEmployee();
        given(employeeRepositoryPort.save(any(Employee.class))).willReturn(saved);

        Employee result = employeeService.hireEmployee("Alice", appUserId, teamId);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        verify(employeeRepositoryPort).save(any(Employee.class));
    }

    // ── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_returnsEmployee_whenFound() {
        Employee employee = sampleEmployee();
        given(employeeRepositoryPort.findById(employee.getId())).willReturn(Optional.of(employee));

        Employee result = employeeService.get(employee.getId());

        assertThat(result).isEqualTo(employee);
    }

    @Test
    void get_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(employeeRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.get(id))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAllEmployees() {
        List<Employee> employees = List.of(sampleEmployee(), Employee.hire("Bob", UUID.randomUUID(), teamId));
        given(employeeRepositoryPort.findAll()).willReturn(employees);

        List<Employee> result = employeeService.list();

        assertThat(result).hasSize(2);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_changesNameAndTeam() {
        Employee existing = sampleEmployee();
        UUID newTeamId = UUID.randomUUID();
        given(employeeRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(employeeRepositoryPort.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));

        Employee updated = employeeService.update(existing.getId(), "Alice Updated", newTeamId, EmployeeStatus.INACTIVE);

        assertThat(updated.getName()).isEqualTo("Alice Updated");
        assertThat(updated.getTeamId()).isEqualTo(newTeamId);
        assertThat(updated.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
    }

    @Test
    void update_keepsExistingStatus_whenStatusIsNull() {
        Employee existing = sampleEmployee();
        given(employeeRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(employeeRepositoryPort.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));

        Employee updated = employeeService.update(existing.getId(), "Alice", teamId, null);

        assertThat(updated.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void update_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(employeeRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.update(id, "Name", teamId, null))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_succeeds_whenEmployeeExists() {
        Employee employee = sampleEmployee();
        given(employeeRepositoryPort.findById(employee.getId())).willReturn(Optional.of(employee));

        employeeService.delete(employee.getId());

        verify(employeeRepositoryPort).deleteById(employee.getId());
    }

    @Test
    void delete_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(employeeRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.delete(id))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
