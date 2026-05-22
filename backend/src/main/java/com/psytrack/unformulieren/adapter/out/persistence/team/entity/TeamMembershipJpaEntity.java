package com.psytrack.unformulieren.adapter.out.persistence.team.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"team_id", "employee_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMembershipJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    private TeamMembershipJpaEntity(UUID id, UUID teamId, UUID employeeId, Instant joinedAt) {
        this.id = id;
        this.teamId = teamId;
        this.employeeId = employeeId;
        this.joinedAt = joinedAt;
    }

    public static TeamMembershipJpaEntity of(UUID id, UUID teamId, UUID employeeId, Instant joinedAt) {
        return new TeamMembershipJpaEntity(id, teamId, employeeId, joinedAt);
    }
}
