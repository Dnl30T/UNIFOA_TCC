package com.psytrack.unformulieren.adapter.out.persistence.teamResult.mapper;

import java.util.Map;
import java.util.Objects;

import com.psytrack.unformulieren.adapter.out.persistence.teamResult.entity.TeamResultJpaEntity;
import com.psytrack.unformulieren.domain.enums.RiskLevel;
import com.psytrack.unformulieren.domain.model.TeamResult;

public class TeamResultPersistenceMapper {

    public TeamResultJpaEntity toJpa(TeamResult domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        return TeamResultJpaEntity.of(
                domain.getId(),
                domain.getTeamId(),
                domain.getFormId(),
                domain.getAverageScore(),
                domain.getRiskLevelDistribution(),
                domain.getCalculatedAt());
    }

    public TeamResult toDomain(TeamResultJpaEntity jpa) {
        Objects.requireNonNull(jpa, "jpa must not be null");
        Map<RiskLevel, Integer> distribution = jpa.getRiskLevelDistribution() == null
                ? Map.of()
                : jpa.getRiskLevelDistribution();

        return TeamResult.reconstitute(
                jpa.getId(),
                jpa.getTeamId(),
                jpa.getFormId(),
                jpa.getAverageScore(),
                distribution,
                jpa.getCalculatedAt());
    }
}
