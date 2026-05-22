package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.RiskLevel;

public class Report {

    private final UUID id;
    private final UUID formId;
    private final UUID teamId;
    private final String name;
    private final List<UUID> respondentIds;
    private final int respondentCount;
    private final double averageScore;
    private final RiskLevel generalRisk;
    private final Map<RiskLevel, Integer> riskDistribution;
    private final Instant generatedAt;
    private final String createdBy;

    public Report(UUID id, UUID formId, UUID teamId, String name, List<UUID> respondentIds,
                  int respondentCount, double averageScore, RiskLevel generalRisk,
                  Map<RiskLevel, Integer> riskDistribution, Instant generatedAt, String createdBy) {
        this.id = id;
        this.formId = formId;
        this.teamId = teamId;
        this.name = name;
        this.respondentIds = List.copyOf(respondentIds);
        this.respondentCount = respondentCount;
        this.averageScore = averageScore;
        this.generalRisk = generalRisk;
        this.riskDistribution = Map.copyOf(riskDistribution);
        this.generatedAt = generatedAt;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public UUID getFormId() { return formId; }
    public UUID getTeamId() { return teamId; }
    public String getName() { return name; }
    public List<UUID> getRespondentIds() { return respondentIds; }
    public int getRespondentCount() { return respondentCount; }
    public double getAverageScore() { return averageScore; }
    public RiskLevel getGeneralRisk() { return generalRisk; }
    public Map<RiskLevel, Integer> getRiskDistribution() { return riskDistribution; }
    public Instant getGeneratedAt() { return generatedAt; }
    public String getCreatedBy() { return createdBy; }
}
