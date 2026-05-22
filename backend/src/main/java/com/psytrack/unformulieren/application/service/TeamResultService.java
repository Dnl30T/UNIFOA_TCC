package com.psytrack.unformulieren.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.TeamResultRepositoryPort;
import com.psytrack.unformulieren.domain.enums.RiskLevel;
import com.psytrack.unformulieren.domain.exception.TeamResultNotFoundException;
import com.psytrack.unformulieren.domain.model.TeamResult;

@Service
public class TeamResultService {

    private final TeamResultRepositoryPort teamResultRepositoryPort;

    public TeamResultService(TeamResultRepositoryPort teamResultRepositoryPort) {
        this.teamResultRepositoryPort = teamResultRepositoryPort;
    }

    public TeamResult create(UUID teamId, UUID formId, double averageScore, Map<RiskLevel, Integer> riskLevelDistribution, Instant calculatedAt) {
        return teamResultRepositoryPort.save(new TeamResult(teamId, formId, averageScore, riskLevelDistribution, calculatedAt));
    }

    public TeamResult get(UUID id) {
        return teamResultRepositoryPort.findById(id)
                .orElseThrow(() -> new TeamResultNotFoundException(id));
    }

    public TeamResult getByTeamId(UUID teamId) {
        return teamResultRepositoryPort.findByTeamId(teamId)
                .orElseThrow(() -> new TeamResultNotFoundException(teamId));
    }

    public TeamResult getByFormId(UUID formId) {
        return teamResultRepositoryPort.findByFormId(formId)
                .orElseThrow(() -> new TeamResultNotFoundException(formId));
    }

    public List<TeamResult> list() {
        return teamResultRepositoryPort.findAll();
    }

    public TeamResult update(UUID id, UUID teamId, UUID formId, double averageScore, Map<RiskLevel, Integer> riskLevelDistribution, Instant calculatedAt) {
        get(id);
        return teamResultRepositoryPort.save(TeamResult.reconstitute(id, teamId, formId, averageScore, riskLevelDistribution, calculatedAt));
    }

    public void delete(UUID id) {
        get(id);
        teamResultRepositoryPort.deleteById(id);
    }
}
