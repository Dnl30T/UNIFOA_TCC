package com.psytrack.unformulieren.adapter.out.persistence.team.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.psytrack.unformulieren.adapter.out.persistence.team.entity.TeamMembershipJpaEntity;
import com.psytrack.unformulieren.domain.model.TeamMembership;

@Component
public class TeamMembershipPersistenceMapper {

    public TeamMembershipJpaEntity toJpa(TeamMembership domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        return TeamMembershipJpaEntity.of(
                domain.getId(),
                domain.getTeamId(),
                domain.getEmployeeId(),
                domain.getJoinedAt());
    }

    public TeamMembership toDomain(TeamMembershipJpaEntity jpa) {
        Objects.requireNonNull(jpa, "jpa must not be null");
        return TeamMembership.reconstitute(
                jpa.getId(),
                jpa.getTeamId(),
                jpa.getEmployeeId(),
                jpa.getJoinedAt());
    }
}
