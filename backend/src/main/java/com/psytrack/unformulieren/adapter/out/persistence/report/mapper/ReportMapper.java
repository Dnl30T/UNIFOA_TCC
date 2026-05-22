package com.psytrack.unformulieren.adapter.out.persistence.report.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.psytrack.unformulieren.adapter.out.persistence.report.entity.ReportEntity;
import com.psytrack.unformulieren.adapter.out.persistence.report.entity.ReportPrimaryKey;
import com.psytrack.unformulieren.domain.enums.RiskLevel;
import com.psytrack.unformulieren.domain.model.Report;

public class ReportMapper {

    public ReportEntity toEntity(Report r) {
        Map<String, Integer> riskDist = r.getRiskDistribution().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));

        return new ReportEntity(
                new ReportPrimaryKey(r.getTeamId(), r.getId()),
                r.getFormId(),
                r.getName(),
                r.getRespondentIds(),
                r.getRespondentCount(),
                r.getAverageScore(),
                r.getGeneralRisk().name(),
                riskDist,
                r.getGeneratedAt(),
                r.getCreatedBy());
    }

    public Report toDomain(ReportEntity e) {
        Map<RiskLevel, Integer> riskDist = e.getRiskDistribution() == null
                ? Map.of()
                : e.getRiskDistribution().entrySet().stream()
                        .collect(Collectors.toMap(entry -> RiskLevel.valueOf(entry.getKey()), Map.Entry::getValue));

        List<UUID> respondentIds = e.getRespondentIds() == null ? List.of() : e.getRespondentIds();

        return new Report(
                e.getKey().getId(),
                e.getFormId(),
                e.getKey().getTeamId(),
                e.getName(),
                respondentIds,
                e.getRespondentCount(),
                e.getAverageScore(),
                e.getGeneralRisk() == null ? RiskLevel.LOW : RiskLevel.valueOf(e.getGeneralRisk()),
                riskDist,
                e.getGeneratedAt(),
                e.getCreatedBy());
    }
}
