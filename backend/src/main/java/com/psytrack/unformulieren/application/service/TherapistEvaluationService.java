package com.psytrack.unformulieren.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.psytrack.unformulieren.application.port.out.TherapistEvaluationRepositoryPort;
import com.psytrack.unformulieren.domain.enums.TherapistEvaluationStatus;
import com.psytrack.unformulieren.domain.model.TherapistEvaluation;

@Service
public class TherapistEvaluationService {

    private final TherapistEvaluationRepositoryPort repository;

    public TherapistEvaluationService(TherapistEvaluationRepositoryPort repository) {
        this.repository = repository;
    }

    public TherapistEvaluation upsert(TherapistEvaluation incoming) {
        Instant now = Instant.now();
        Optional<TherapistEvaluation> existing =
                repository.findByFormIdAndEmployeeId(incoming.getFormId(), incoming.getEmployeeId());

        if (existing.map(e -> e.getStatus() == TherapistEvaluationStatus.PUBLISHED).orElse(false)) {
            throw new IllegalArgumentException("Published analyses cannot be edited");
        }

        Instant createdAt = existing.map(TherapistEvaluation::getCreatedAt).orElse(now);

        TherapistEvaluation toSave = new TherapistEvaluation(
                incoming.getFormId(), incoming.getEmployeeId(),
                incoming.getOccupationalContext(), incoming.getTeamDynamics(),
                incoming.getPsychosomaticSymptoms(), incoming.getCognitiveEmotionalChanges(),
                incoming.getExhaustionScore(), incoming.getExhaustionJustification(),
                incoming.getDepersonalizationScore(), incoming.getDepersonalizationJustification(),
                incoming.getDifferentialDiagnosis(), incoming.getInterventionPlan(),
                incoming.getInternalNote(), incoming.getHrSummary(),
                incoming.getClosingCommentary(),
                incoming.getStressScore(), incoming.getSleepScore(), incoming.getOverloadScore(),
                incoming.getFatigueScore(), incoming.getDisengagementScore(), incoming.getIsolationScore(),
                TherapistEvaluationStatus.DRAFT,
                existing.map(TherapistEvaluation::getPublishedAt).orElse(null),
                incoming.getCreatedBy(), createdAt, now);

        return repository.save(toSave);
    }

    public TherapistEvaluation publish(UUID formId, UUID employeeId) {
        TherapistEvaluation evaluation = repository.findByFormIdAndEmployeeId(formId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found for publish"));

        if (evaluation.getClosingCommentary() == null || evaluation.getClosingCommentary().isBlank()) {
            throw new IllegalArgumentException("Closing commentary is required before publishing analysis");
        }

        TherapistEvaluation published = new TherapistEvaluation(
                evaluation.getFormId(), evaluation.getEmployeeId(),
                evaluation.getOccupationalContext(), evaluation.getTeamDynamics(),
                evaluation.getPsychosomaticSymptoms(), evaluation.getCognitiveEmotionalChanges(),
                evaluation.getExhaustionScore(), evaluation.getExhaustionJustification(),
                evaluation.getDepersonalizationScore(), evaluation.getDepersonalizationJustification(),
                evaluation.getDifferentialDiagnosis(), evaluation.getInterventionPlan(),
                evaluation.getInternalNote(), evaluation.getHrSummary(),
                evaluation.getClosingCommentary(),
                evaluation.getStressScore(), evaluation.getSleepScore(), evaluation.getOverloadScore(),
                evaluation.getFatigueScore(), evaluation.getDisengagementScore(), evaluation.getIsolationScore(),
                TherapistEvaluationStatus.PUBLISHED,
                Instant.now(),
                evaluation.getCreatedBy(), evaluation.getCreatedAt(), Instant.now());
        return repository.save(published);
    }

    public List<TherapistEvaluation> publishBatch(UUID formId, List<UUID> employeeIds) {
        List<TherapistEvaluation> published = new ArrayList<>();
        for (UUID employeeId : employeeIds) {
            try {
                published.add(publish(formId, employeeId));
            } catch (IllegalArgumentException ignored) {
                // Keep incomplete drafts untouched as requested.
            }
        }
        return published;
    }

    public Optional<TherapistEvaluation> findPublished(UUID formId, UUID employeeId) {
        return repository.findByFormIdAndEmployeeId(formId, employeeId)
                .filter(evaluation -> evaluation.getStatus() == TherapistEvaluationStatus.PUBLISHED);
    }

    public Optional<TherapistEvaluation> find(UUID formId, UUID employeeId) {
        return repository.findByFormIdAndEmployeeId(formId, employeeId);
    }

    public List<TherapistEvaluation> findAllByFormId(UUID formId) {
        return repository.findAllByFormId(formId);
    }

    public List<TherapistEvaluation> findPublishedByFormId(UUID formId) {
        return repository.findPublishedByFormId(formId);
    }
}
