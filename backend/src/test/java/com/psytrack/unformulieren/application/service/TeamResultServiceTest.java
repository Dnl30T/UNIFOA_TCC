package com.psytrack.unformulieren.application.service;

import com.psytrack.unformulieren.application.port.out.TeamResultRepositoryPort;
import com.psytrack.unformulieren.domain.enums.RiskLevel;
import com.psytrack.unformulieren.domain.exception.TeamResultNotFoundException;
import com.psytrack.unformulieren.domain.model.TeamResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeamResultServiceTest {

    @Mock private TeamResultRepositoryPort teamResultRepositoryPort;

    @InjectMocks private TeamResultService teamResultService;

    private final UUID teamId = UUID.randomUUID();
    private final UUID formId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private TeamResult sampleResult() {
        return new TeamResult(teamId, formId, 72.5, Map.of(RiskLevel.LOW, 5, RiskLevel.MEDIUM, 3), now);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsResult() {
        TeamResult saved = sampleResult();
        given(teamResultRepositoryPort.save(any(TeamResult.class))).willReturn(saved);

        TeamResult result = teamResultService.create(
                teamId, formId, 72.5, Map.of(RiskLevel.LOW, 5, RiskLevel.MEDIUM, 3), now);

        assertThat(result.getAverageScore()).isEqualTo(72.5);
        verify(teamResultRepositoryPort).save(any(TeamResult.class));
    }

    // ── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_returnsResult_whenFound() {
        TeamResult result = sampleResult();
        given(teamResultRepositoryPort.findById(result.getId())).willReturn(Optional.of(result));

        assertThat(teamResultService.get(result.getId())).isEqualTo(result);
    }

    @Test
    void get_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(teamResultRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamResultService.get(id))
                .isInstanceOf(TeamResultNotFoundException.class);
    }

    // ── getByTeamId ──────────────────────────────────────────────────────────

    @Test
    void getByTeamId_returnsResult_whenFound() {
        TeamResult result = sampleResult();
        given(teamResultRepositoryPort.findByTeamId(teamId)).willReturn(Optional.of(result));

        assertThat(teamResultService.getByTeamId(teamId)).isEqualTo(result);
    }

    @Test
    void getByTeamId_throws_whenNotFound() {
        given(teamResultRepositoryPort.findByTeamId(teamId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamResultService.getByTeamId(teamId))
                .isInstanceOf(TeamResultNotFoundException.class);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAll() {
        given(teamResultRepositoryPort.findAll()).willReturn(List.of(sampleResult()));

        assertThat(teamResultService.list()).hasSize(1);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_savesUpdatedResult() {
        TeamResult existing = sampleResult();
        given(teamResultRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(teamResultRepositoryPort.save(any(TeamResult.class))).willAnswer(inv -> inv.getArgument(0));

        TeamResult updated = teamResultService.update(
                existing.getId(), teamId, formId, 80.0, Map.of(RiskLevel.LOW, 8), now);

        assertThat(updated.getAverageScore()).isEqualTo(80.0);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_succeeds_whenResultExists() {
        TeamResult result = sampleResult();
        given(teamResultRepositoryPort.findById(result.getId())).willReturn(Optional.of(result));

        teamResultService.delete(result.getId());

        verify(teamResultRepositoryPort).deleteById(result.getId());
    }

    @Test
    void delete_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(teamResultRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamResultService.delete(id))
                .isInstanceOf(TeamResultNotFoundException.class);
    }
}
