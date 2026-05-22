package com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.mapper;

import com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.entity.TherapistEvaluationEntity;
import com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.entity.TherapistEvaluationPrimaryKey;
import com.psytrack.unformulieren.domain.enums.TherapistEvaluationStatus;
import com.psytrack.unformulieren.domain.model.TherapistEvaluation;

public class TherapistEvaluationMapper {

    public TherapistEvaluationEntity toEntity(TherapistEvaluation d) {
        return new TherapistEvaluationEntity(
                new TherapistEvaluationPrimaryKey(d.getFormId(), d.getEmployeeId()),
                d.getOccupationalContext(),
                d.getTeamDynamics(),
                d.getPsychosomaticSymptoms(),
                d.getCognitiveEmotionalChanges(),
                d.getExhaustionScore(),
                d.getExhaustionJustification(),
                d.getDepersonalizationScore(),
                d.getDepersonalizationJustification(),
                d.getDifferentialDiagnosis(),
                d.getInterventionPlan(),
                d.getInternalNote(),
                d.getHrSummary(),
                d.getClosingCommentary(),
                d.getStressScore(),
                d.getSleepScore(),
                d.getOverloadScore(),
                d.getFatigueScore(),
                d.getDisengagementScore(),
                d.getIsolationScore(),
                d.getStatus().name(),
                d.getPublishedAt(),
                d.getCreatedBy(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }

    public TherapistEvaluation toDomain(TherapistEvaluationEntity e) {
        return new TherapistEvaluation(
                e.getKey().getFormId(),
                e.getKey().getEmployeeId(),
                e.getOccupationalContext(),
                e.getTeamDynamics(),
                e.getPsychosomaticSymptoms(),
                e.getCognitiveEmotionalChanges(),
                e.getExhaustionScore(),
                e.getExhaustionJustification(),
                e.getDepersonalizationScore(),
                e.getDepersonalizationJustification(),
                e.getDifferentialDiagnosis(),
                e.getInterventionPlan(),
                e.getInternalNote(),
                e.getHrSummary(),
                e.getClosingCommentary(),
                e.getStressScore(),
                e.getSleepScore(),
                e.getOverloadScore(),
                e.getFatigueScore(),
                e.getDisengagementScore(),
                e.getIsolationScore(),
                e.getStatus() == null ? TherapistEvaluationStatus.DRAFT : TherapistEvaluationStatus.valueOf(e.getStatus()),
                e.getPublishedAt(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
