package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate representing a team within the application.
 *
 * A team has:
 * - A single manager (acts as the team's creator/owner).
 * - A single counselor (conducts forms for the team).
 * - Multiple employees (team members responding to forms).
 *
 * The manager and counselor are always AppUser instances with specific roles.
 */
public class Team {

    private final UUID id;
    private final String name;
    private UUID managerId;
    private UUID counselorId;
    private String teamCode;
    private final Instant createdAt;
    private Instant updatedAt;

    public Team(String name, UUID managerId, String teamCode) {
        this(UUID.randomUUID(), name, managerId, null, teamCode, Instant.now(), Instant.now());
    }

    private Team(UUID id, String name, UUID managerId, UUID counselorId,
                 String teamCode, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.trim();
        this.managerId = Objects.requireNonNull(managerId, "managerId must not be null");
        this.counselorId = counselorId;
        this.teamCode = Objects.requireNonNull(teamCode, "teamCode must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Team reconstitute(UUID id, String name, UUID managerId, UUID counselorId,
                                     String teamCode, Instant createdAt, Instant updatedAt) {
        return new Team(id, name, managerId, counselorId, teamCode, createdAt, updatedAt);
    }

    public void assignCounselor(UUID newCounselorId) {
        this.counselorId = Objects.requireNonNull(newCounselorId, "counselorId must not be null");
        this.updatedAt = Instant.now();
    }

    public void changeManager(UUID newManagerId) {
        this.managerId = Objects.requireNonNull(newManagerId, "managerId must not be null");
        this.updatedAt = Instant.now();
    }

    public UUID getId()          { return id; }
    public String getName()      { return name; }
    public UUID getManagerId()   { return managerId; }
    public UUID getCounselorId() { return counselorId; }
    public String getTeamCode()  { return teamCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean hasCounselor() { return counselorId != null; }
}
