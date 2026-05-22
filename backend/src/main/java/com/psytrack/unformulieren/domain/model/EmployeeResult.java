package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.RiskLevel;

public class EmployeeResult {

    private final UUID id;

    private final UUID employeeId;

    private final UUID formId;

    private final int helperScore;

    private final Integer finalScore;

    private final RiskLevel riskLevel;

    private final Instant calculatedAt;

    public EmployeeResult(UUID employeeId, UUID formId, int helperScore, Integer finalScore, RiskLevel riskLevel, Instant calculatedAt) {
        this(UUID.randomUUID(), employeeId, formId, helperScore, finalScore, riskLevel, calculatedAt);
    }

    private EmployeeResult(UUID id, UUID employeeId, UUID formId, int helperScore, Integer finalScore, RiskLevel riskLevel, Instant calculatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.formId = Objects.requireNonNull(formId, "formId must not be null");
        if (helperScore < 0) {
            throw new IllegalArgumentException("helperScore must be greater than or equal to 0");
        }
        if (finalScore != null && finalScore < 0) {
            throw new IllegalArgumentException("finalScore must be greater than or equal to 0");
        }
        this.helperScore = helperScore;
        this.finalScore = finalScore;
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
    }

    public static EmployeeResult reconstitute(UUID id, UUID employeeId, UUID formId, int helperScore, Integer finalScore,
                                              RiskLevel riskLevel, Instant calculatedAt) {
        return new EmployeeResult(id, employeeId, formId, helperScore, finalScore, riskLevel, calculatedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public UUID getFormId() {
        return formId;
    }

    public int getHelperScore() {
        return helperScore;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public int getDisplayScore() {
        return finalScore != null ? finalScore : helperScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

}
