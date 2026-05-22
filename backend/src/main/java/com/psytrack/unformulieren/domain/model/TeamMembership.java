package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an employee's membership in a team.
 *
 * Tracks when an employee joined a team and provides audit information.
 */
public class TeamMembership {

    private final UUID id;
    private final UUID teamId;
    private final UUID employeeId;
    private final Instant joinedAt;

    public TeamMembership(UUID teamId, UUID employeeId) {
        this(UUID.randomUUID(), teamId, employeeId, Instant.now());
    }

    private TeamMembership(UUID id, UUID teamId, UUID employeeId, Instant joinedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.teamId = Objects.requireNonNull(teamId, "teamId must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt must not be null");
    }

    public static TeamMembership reconstitute(UUID id, UUID teamId, UUID employeeId, Instant joinedAt) {
        return new TeamMembership(id, teamId, employeeId, joinedAt);
    }

    public UUID getId() { return id; }
    public UUID getTeamId() { return teamId; }
    public UUID getEmployeeId() { return employeeId; }
    public Instant getJoinedAt() { return joinedAt; }
}
