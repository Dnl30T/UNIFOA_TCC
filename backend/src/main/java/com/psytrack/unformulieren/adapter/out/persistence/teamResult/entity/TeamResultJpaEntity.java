package com.psytrack.unformulieren.adapter.out.persistence.teamResult.entity;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.RiskLevel;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamResultJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(name = "average_score", nullable = false)
    private double averageScore;

    @ElementCollection
    @CollectionTable(name = "team_result_risk_level_distribution", joinColumns = @JoinColumn(name = "team_result_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "risk_level", length = 24)
    @Column(name = "quantity", nullable = false)
    private Map<RiskLevel, Integer> riskLevelDistribution;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    private TeamResultJpaEntity(UUID id, UUID teamId, UUID formId, double averageScore,
                                Map<RiskLevel, Integer> riskLevelDistribution, Instant calculatedAt) {
        this.id = id;
        this.teamId = teamId;
        this.formId = formId;
        this.averageScore = averageScore;
        this.riskLevelDistribution = new EnumMap<>(riskLevelDistribution);
        this.calculatedAt = calculatedAt;
    }

    public static TeamResultJpaEntity of(UUID id, UUID teamId, UUID formId, double averageScore,
                                         Map<RiskLevel, Integer> riskLevelDistribution, Instant calculatedAt) {
        return new TeamResultJpaEntity(id, teamId, formId, averageScore, riskLevelDistribution, calculatedAt);
    }
}
