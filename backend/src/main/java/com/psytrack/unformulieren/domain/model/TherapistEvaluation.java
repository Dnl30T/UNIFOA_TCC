package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.TherapistEvaluationStatus;

public class TherapistEvaluation {

    private final UUID formId;
    private final UUID employeeId;

    // Module 1 – Stressors
    private final String occupationalContext;
    private final String teamDynamics;

    // Module 2 – Symptomatology
    private final String psychosomaticSymptoms;
    private final String cognitiveEmotionalChanges;

    // Module 3 – Dimensions
    private final Integer exhaustionScore;
    private final String exhaustionJustification;
    private final Integer depersonalizationScore;
    private final String depersonalizationJustification;

    // Module 4 – Clinical opinion
    private final String differentialDiagnosis;
    private final String interventionPlan;

    // Module 5 – Visibility
    private final String internalNote;
    private final String hrSummary;
    private final String closingCommentary;

    // Risk overview categories (0-100 each)
    private final Integer stressScore;
    private final Integer sleepScore;
    private final Integer overloadScore;
    private final Integer fatigueScore;
    private final Integer disengagementScore;
    private final Integer isolationScore;

    private final TherapistEvaluationStatus status;
    private final Instant publishedAt;

    // Metadata
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TherapistEvaluation(
            UUID formId, UUID employeeId,
            String occupationalContext, String teamDynamics,
            String psychosomaticSymptoms, String cognitiveEmotionalChanges,
            Integer exhaustionScore, String exhaustionJustification,
            Integer depersonalizationScore, String depersonalizationJustification,
            String differentialDiagnosis, String interventionPlan,
            String internalNote, String hrSummary,
            String closingCommentary,
            Integer stressScore, Integer sleepScore, Integer overloadScore,
            Integer fatigueScore, Integer disengagementScore, Integer isolationScore,
            TherapistEvaluationStatus status, Instant publishedAt,
            String createdBy, Instant createdAt, Instant updatedAt) {
        this.formId = formId;
        this.employeeId = employeeId;
        this.occupationalContext = occupationalContext;
        this.teamDynamics = teamDynamics;
        this.psychosomaticSymptoms = psychosomaticSymptoms;
        this.cognitiveEmotionalChanges = cognitiveEmotionalChanges;
        this.exhaustionScore = exhaustionScore;
        this.exhaustionJustification = exhaustionJustification;
        this.depersonalizationScore = depersonalizationScore;
        this.depersonalizationJustification = depersonalizationJustification;
        this.differentialDiagnosis = differentialDiagnosis;
        this.interventionPlan = interventionPlan;
        this.internalNote = internalNote;
        this.hrSummary = hrSummary;
        this.closingCommentary = closingCommentary;
        this.stressScore = clampScore(stressScore);
        this.sleepScore = clampScore(sleepScore);
        this.overloadScore = clampScore(overloadScore);
        this.fatigueScore = clampScore(fatigueScore);
        this.disengagementScore = clampScore(disengagementScore);
        this.isolationScore = clampScore(isolationScore);
        this.status = status == null ? TherapistEvaluationStatus.DRAFT : status;
        this.publishedAt = publishedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private Integer clampScore(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Risk overview category scores must be between 0 and 100");
        }
        return value;
    }

    public UUID getFormId() { return formId; }
    public UUID getEmployeeId() { return employeeId; }
    public String getOccupationalContext() { return occupationalContext; }
    public String getTeamDynamics() { return teamDynamics; }
    public String getPsychosomaticSymptoms() { return psychosomaticSymptoms; }
    public String getCognitiveEmotionalChanges() { return cognitiveEmotionalChanges; }
    public Integer getExhaustionScore() { return exhaustionScore; }
    public String getExhaustionJustification() { return exhaustionJustification; }
    public Integer getDepersonalizationScore() { return depersonalizationScore; }
    public String getDepersonalizationJustification() { return depersonalizationJustification; }
    public String getDifferentialDiagnosis() { return differentialDiagnosis; }
    public String getInterventionPlan() { return interventionPlan; }
    public String getInternalNote() { return internalNote; }
    public String getHrSummary() { return hrSummary; }
    public String getClosingCommentary() { return closingCommentary; }
    public Integer getStressScore() { return stressScore; }
    public Integer getSleepScore() { return sleepScore; }
    public Integer getOverloadScore() { return overloadScore; }
    public Integer getFatigueScore() { return fatigueScore; }
    public Integer getDisengagementScore() { return disengagementScore; }
    public Integer getIsolationScore() { return isolationScore; }
    public TherapistEvaluationStatus getStatus() { return status; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
