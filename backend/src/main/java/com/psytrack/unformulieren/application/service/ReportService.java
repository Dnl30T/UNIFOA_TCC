package com.psytrack.unformulieren.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.EmployeeResultRepositoryPort;
import com.psytrack.unformulieren.application.port.out.ReportRepositoryPort;
import com.psytrack.unformulieren.application.port.out.TherapistEvaluationRepositoryPort;
import com.psytrack.unformulieren.domain.enums.RiskLevel;
import com.psytrack.unformulieren.domain.model.EmployeeResult;
import com.psytrack.unformulieren.domain.model.Report;
import com.psytrack.unformulieren.domain.model.TherapistEvaluation;

@Service
public class ReportService {

    private final ReportRepositoryPort reportRepository;
    private final TherapistEvaluationRepositoryPort evaluationRepository;
    private final EmployeeResultRepositoryPort resultRepository;

    public ReportService(ReportRepositoryPort reportRepository,
                         TherapistEvaluationRepositoryPort evaluationRepository,
                         EmployeeResultRepositoryPort resultRepository) {
        this.reportRepository = reportRepository;
        this.evaluationRepository = evaluationRepository;
        this.resultRepository = resultRepository;
    }

    /**
     * Generates a new Report by aggregating all respondents for the given form that have:
     *   1. An EmployeeResult (score + riskLevel)
     *   2. A PUBLISHED TherapistEvaluation with non-blank closingCommentary
     *
     * Metrics are calculated once and frozen in the stored Report record.
     */
    public Report generate(UUID formId, UUID teamId, String reportName, String createdBy) {
        // Fetch all published evaluations with closing commentary for this form
        List<TherapistEvaluation> completeEvals = evaluationRepository.findPublishedByFormId(formId)
                .stream()
                .filter(e -> e.getClosingCommentary() != null && !e.getClosingCommentary().isBlank())
                .collect(Collectors.toList());

        if (completeEvals.isEmpty()) {
            throw new IllegalStateException("No complete published analyses found for this form. " +
                    "All employees must have a published analysis with closing commentary before generating a report.");
        }

        // Collect respondents: must have both EmployeeResult AND published evaluation
        List<UUID> respondentIds = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        List<RiskLevel> riskLevels = new ArrayList<>();

        for (TherapistEvaluation eval : completeEvals) {
            Optional<EmployeeResult> resultOpt = resultRepository.findByEmployeeIdAndFormId(
                    eval.getEmployeeId(), formId);

            resultOpt.ifPresent(result -> {
                respondentIds.add(eval.getEmployeeId());
                int score = result.getFinalScore() != null ? result.getFinalScore() : result.getHelperScore();
                scores.add(score);
                riskLevels.add(result.getRiskLevel());
            });
        }

        if (respondentIds.isEmpty()) {
            throw new IllegalStateException("No respondents with complete data (score + published analysis) found.");
        }

        // Calculate metrics
        double averageScore = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        Map<RiskLevel, Integer> riskDistribution = new HashMap<>();
        for (RiskLevel rl : RiskLevel.values()) {
            riskDistribution.put(rl, (int) riskLevels.stream().filter(r -> r == rl).count());
        }

        RiskLevel generalRisk = computeGeneralRisk(riskLevels);

        Report report = new Report(
                UUID.randomUUID(),
                formId,
                teamId,
                reportName,
                respondentIds,
                respondentIds.size(),
                Math.round(averageScore * 10.0) / 10.0,
                generalRisk,
                riskDistribution,
                Instant.now(),
                createdBy);

        return reportRepository.save(report);
    }

    public List<Report> findByTeamId(UUID teamId) {
        return reportRepository.findByTeamId(teamId);
    }

    public List<Report> findByFormId(UUID formId) {
        return reportRepository.findByFormId(formId);
    }

    public Optional<Report> findById(UUID teamId, UUID reportId) {
        return reportRepository.findById(teamId, reportId);
    }

    /** Returns the mode RiskLevel from the list; if empty defaults to MEDIUM. */
    private RiskLevel computeGeneralRisk(List<RiskLevel> levels) {
        if (levels.isEmpty()) return RiskLevel.MEDIUM;
        return levels.stream()
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(RiskLevel.MEDIUM);
    }
}
