package com.psytrack.unformulieren.adapter.out.persistence.employee.entity;

import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.EmployeeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "app_user_id", nullable = false, updatable = false)
    private UUID appUserId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EmployeeStatus status;

    private EmployeeJpaEntity(UUID id, String name, UUID appUserId, UUID teamId, EmployeeStatus status) {
        this.id = id;
        this.name = name;
        this.appUserId = appUserId;
        this.teamId = teamId;
        this.status = status;
    }

    public static EmployeeJpaEntity of(UUID id, String name, UUID appUserId, UUID teamId, EmployeeStatus status) {
        return new EmployeeJpaEntity(id, name, appUserId, teamId, status);
    }
}
