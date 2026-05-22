package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.RiskLevel;


public class TeamResult {

    private final UUID id;

    private final UUID teamId;

    private final UUID formId;

    private final double averageScore;

    private final Map<RiskLevel, Integer> riskLevelDistribution;

    private final Instant calculatedAt;

    public TeamResult(UUID teamId, UUID formId, double averageScore,
                      Map<RiskLevel, Integer> riskLevelDistribution, Instant calculatedAt) {
        this(UUID.randomUUID(), teamId, formId, averageScore, riskLevelDistribution, calculatedAt);
    }

    private TeamResult(UUID id, UUID teamId, UUID formId, double averageScore,
                       Map<RiskLevel, Integer> riskLevelDistribution, Instant calculatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.teamId = Objects.requireNonNull(teamId, "teamId must not be null");
        this.formId = Objects.requireNonNull(formId, "formId must not be null");
        if (averageScore < 0.0) {
            throw new IllegalArgumentException("averageScore must be non-negative");
        }
        this.averageScore = averageScore;
        this.riskLevelDistribution = new EnumMap<>(Objects.requireNonNull(riskLevelDistribution,
                "riskLevelDistribution must not be null"));
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
    }

    public static TeamResult reconstitute(UUID id, UUID teamId, UUID formId, double averageScore,
                                          Map<RiskLevel, Integer> riskLevelDistribution, Instant calculatedAt) {
        return new TeamResult(id, teamId, formId, averageScore, riskLevelDistribution, calculatedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public UUID getFormId() {
        return formId;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public Map<RiskLevel, Integer> getRiskLevelDistribution() {
        return Map.copyOf(riskLevelDistribution);
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

}
