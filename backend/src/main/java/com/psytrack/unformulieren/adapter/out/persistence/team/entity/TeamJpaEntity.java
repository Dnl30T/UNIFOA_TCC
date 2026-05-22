package com.psytrack.unformulieren.adapter.out.persistence.team.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "manager_id", nullable = false)
    private UUID managerId;

    @Column(name = "counselor_id")
    private UUID counselorId;

    @Column(name = "team_code", nullable = false, unique = true, length = 8)
    private String teamCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private TeamJpaEntity(UUID id, String name, UUID managerId, UUID counselorId,
                          String teamCode, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.managerId = managerId;
        this.counselorId = counselorId;
        this.teamCode = teamCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TeamJpaEntity of(UUID id, String name, UUID managerId, UUID counselorId,
                                   String teamCode, Instant createdAt, Instant updatedAt) {
        return new TeamJpaEntity(id, name, managerId, counselorId, teamCode, createdAt, updatedAt);
    }
}
