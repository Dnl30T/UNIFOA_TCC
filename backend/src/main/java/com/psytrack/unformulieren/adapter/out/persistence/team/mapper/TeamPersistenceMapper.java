package com.psytrack.unformulieren.adapter.out.persistence.team.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.psytrack.unformulieren.adapter.out.persistence.team.entity.TeamJpaEntity;
import com.psytrack.unformulieren.domain.model.Team;

@Component
public class TeamPersistenceMapper {

    public TeamJpaEntity toJpa(Team domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        return TeamJpaEntity.of(
                domain.getId(),
                domain.getName(),
                domain.getManagerId(),
                domain.getCounselorId(),
                domain.getTeamCode(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }

    public Team toDomain(TeamJpaEntity jpa) {
        Objects.requireNonNull(jpa, "jpa must not be null");
        return Team.reconstitute(
                jpa.getId(),
                jpa.getName(),
                jpa.getManagerId(),
                jpa.getCounselorId(),
                jpa.getTeamCode(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt());
    }
}
