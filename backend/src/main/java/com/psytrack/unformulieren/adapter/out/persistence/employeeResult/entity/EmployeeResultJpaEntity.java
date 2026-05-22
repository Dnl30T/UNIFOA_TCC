package com.psytrack.unformulieren.adapter.out.persistence.employeeResult.entity;

import java.time.Instant;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.RiskLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeResultJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(nullable = false)
    private int score;

    @Column(name = "final_score")
    private Integer finalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 24)
    private RiskLevel riskLevel;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    private EmployeeResultJpaEntity(UUID id, UUID employeeId, UUID formId, int score, Integer finalScore, RiskLevel riskLevel, Instant calculatedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.formId = formId;
        this.score = score;
        this.finalScore = finalScore;
        this.riskLevel = riskLevel;
        this.calculatedAt = calculatedAt;
    }

    public static EmployeeResultJpaEntity of(UUID id, UUID employeeId, UUID formId, int score, Integer finalScore, RiskLevel riskLevel, Instant calculatedAt) {
        return new EmployeeResultJpaEntity(id, employeeId, formId, score, finalScore, riskLevel, calculatedAt);
    }
}
