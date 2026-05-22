package com.psytrack.unformulieren.application.service;

import com.psytrack.unformulieren.application.port.out.EmployeeResultRepositoryPort;
import com.psytrack.unformulieren.domain.enums.RiskLevel;
import com.psytrack.unformulieren.domain.exception.EmployeeResultNotFoundException;
import com.psytrack.unformulieren.domain.model.EmployeeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmployeeResultServiceTest {

    @Mock private EmployeeResultRepositoryPort employeeResultRepositoryPort;

    @InjectMocks private EmployeeResultService employeeResultService;

    private final UUID employeeId = UUID.randomUUID();
    private final UUID formId     = UUID.randomUUID();
    private final Instant now     = Instant.now();

    private EmployeeResult sampleResult() {
        return new EmployeeResult(employeeId, formId, 75, RiskLevel.MEDIUM, now);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsResult() {
        EmployeeResult saved = sampleResult();
        given(employeeResultRepositoryPort.save(any(EmployeeResult.class))).willReturn(saved);

        EmployeeResult result = employeeResultService.create(employeeId, formId, 75, RiskLevel.MEDIUM, now);

        assertThat(result.getScore()).isEqualTo(75);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        verify(employeeResultRepositoryPort).save(any(EmployeeResult.class));
    }

    // ── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_returnsResult_whenFound() {
        EmployeeResult result = sampleResult();
        given(employeeResultRepositoryPort.findById(result.getId())).willReturn(Optional.of(result));

        assertThat(employeeResultService.get(result.getId())).isEqualTo(result);
    }

    @Test
    void get_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(employeeResultRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeResultService.get(id))
                .isInstanceOf(EmployeeResultNotFoundException.class);
    }

    // ── getByEmployeeId ──────────────────────────────────────────────────────

    @Test
    void getByEmployeeId_returnsResult_whenFound() {
        EmployeeResult result = sampleResult();
        given(employeeResultRepositoryPort.findByEmployeeId(employeeId)).willReturn(Optional.of(result));

        assertThat(employeeResultService.getByEmployeeId(employeeId)).isEqualTo(result);
    }

    @Test
    void getByEmployeeId_throws_whenNotFound() {
        given(employeeResultRepositoryPort.findByEmployeeId(employeeId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeResultService.getByEmployeeId(employeeId))
                .isInstanceOf(EmployeeResultNotFoundException.class);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAll() {
        given(employeeResultRepositoryPort.findAll()).willReturn(List.of(sampleResult()));

        assertThat(employeeResultService.list()).hasSize(1);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_savesReconstitutedResult() {
        EmployeeResult existing = sampleResult();
        given(employeeResultRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(employeeResultRepositoryPort.save(any(EmployeeResult.class))).willAnswer(inv -> inv.getArgument(0));

        EmployeeResult updated = employeeResultService.update(
                existing.getId(), employeeId, formId, 90, RiskLevel.HIGH, now);

        assertThat(updated.getScore()).isEqualTo(90);
        assertThat(updated.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_succeeds_whenResultExists() {
        EmployeeResult result = sampleResult();
        given(employeeResultRepositoryPort.findById(result.getId())).willReturn(Optional.of(result));

        employeeResultService.delete(result.getId());

        verify(employeeResultRepositoryPort).deleteById(result.getId());
    }

    @Test
    void delete_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(employeeResultRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeResultService.delete(id))
                .isInstanceOf(EmployeeResultNotFoundException.class);
    }
}
