package com.psytrack.unformulieren.domain.model;

import java.util.Objects;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.EmployeeStatus;
import com.psytrack.unformulieren.domain.validation.DomainValidations;

public class Employee {

    private final UUID id;

    private final String name;

    private final UUID appUserId;

    private UUID teamId;

    private EmployeeStatus status;

    public Employee(String name, UUID appUserId, UUID teamId, EmployeeStatus status) {
        this(UUID.randomUUID(), name, appUserId, teamId, status);
    }

    private Employee(UUID id, String name, UUID appUserId, UUID teamId, EmployeeStatus status) {
        this.id = requireNonNull(id, "id");
        this.name = DomainValidations.requireNonBlank(name, "name");
        this.appUserId = requireNonNull(appUserId, "appUserId");
        this.teamId = requireNonNull(teamId, "teamId");
        this.status = requireNonNull(status, "status");
    }

    public static Employee hire(String name, UUID appUserId, UUID teamId) {
        return new Employee(name, appUserId, teamId, EmployeeStatus.ACTIVE);
    }

    public static Employee reconstitute(UUID id, String name, UUID appUserId, UUID teamId, EmployeeStatus status) {
        return new Employee(id, name, appUserId, teamId, status);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getAppUserId() {
        return appUserId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void activate() {
        status = EmployeeStatus.ACTIVE;
    }

    public void deactivate() {
        status = EmployeeStatus.INACTIVE;
    }

    public void changeTeam(UUID newTeamId) {
        teamId = requireNonNull(newTeamId, "newTeamId");
    }

    public boolean isActive() {
        return status == EmployeeStatus.ACTIVE;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }

}
