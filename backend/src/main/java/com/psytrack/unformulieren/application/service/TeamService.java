package com.psytrack.unformulieren.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.TeamMembershipRepositoryPort;
import com.psytrack.unformulieren.application.port.out.TeamRepositoryPort;
import com.psytrack.unformulieren.domain.exception.CounselorAlreadyHasTeamException;
import com.psytrack.unformulieren.domain.exception.DuplicateTeamNameException;
import com.psytrack.unformulieren.domain.exception.ManagerAlreadyHasTeamException;
import com.psytrack.unformulieren.domain.exception.TeamNotFoundException;
import com.psytrack.unformulieren.domain.model.Team;
import com.psytrack.unformulieren.domain.model.TeamMembership;

@Service
public class TeamService {

    private static final int TEAM_CODE_LENGTH = 8;
    private static final int MAX_TEAM_CODE_ATTEMPTS = 25;
    private static final String TEAM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    private final TeamRepositoryPort teamRepositoryPort;
    private final TeamMembershipRepositoryPort membershipRepositoryPort;

    public TeamService(TeamRepositoryPort teamRepositoryPort,
                       TeamMembershipRepositoryPort membershipRepositoryPort) {
        this.teamRepositoryPort = teamRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
    }

    public Team registerTeam(String name, UUID managerId) {
        // Enforce: each manager owns at most one team.
        teamRepositoryPort.findByManagerId(managerId).ifPresent(existing -> {
            throw new ManagerAlreadyHasTeamException(managerId);
        });
        teamRepositoryPort.findByName(name).ifPresent(existing -> {
            throw new DuplicateTeamNameException(name);
        });
        String teamCode = generateUniqueTeamCode();
        return teamRepositoryPort.save(new Team(name, managerId, teamCode));
    }

    public Team get(UUID id) {
        return teamRepositoryPort.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    public List<Team> list() {
        return teamRepositoryPort.findAll();
    }

    public Team findByManagerId(UUID managerId) {
        return teamRepositoryPort.findByManagerId(managerId).orElse(null);
    }

    public Team findByCounselorId(UUID counselorId) {
        return teamRepositoryPort.findByCounselorId(counselorId).stream().findFirst().orElse(null);
    }

    public Team update(UUID id, String name) {
        Team team = get(id);
        teamRepositoryPort.findByName(name).ifPresent(found -> {
            if (!found.getId().equals(id)) {
                throw new DuplicateTeamNameException(name);
            }
        });
        return teamRepositoryPort.save(Team.reconstitute(
                team.getId(),
                name,
                team.getManagerId(),
                team.getCounselorId(),
                team.getTeamCode(),
                team.getCreatedAt(),
                team.getUpdatedAt()));
    }

    public void delete(UUID id) {
        get(id);
        teamRepositoryPort.deleteById(id);
    }

    public Team assignCounselor(UUID teamId, UUID counselorId) {
        Team team = get(teamId);
        // Enforce: each counselor is assigned to at most one team.
        teamRepositoryPort.findByCounselorId(counselorId).stream()
                .filter(t -> !t.getId().equals(teamId))
                .findFirst()
                .ifPresent(existing -> {
                    throw new CounselorAlreadyHasTeamException(counselorId);
                });
        team.assignCounselor(counselorId);
        return teamRepositoryPort.save(team);
    }

    public TeamMembership joinByCode(UUID employeeId, String teamCode) {
        String normalizedCode = normalizeTeamCode(teamCode);
        Team team = teamRepositoryPort.findByTeamCode(normalizedCode)
            .orElseThrow(() -> new TeamNotFoundException(normalizedCode));
        return joinTeam(employeeId, team.getId());
    }

    public TeamMembership joinById(UUID employeeId, UUID teamId) {
        teamRepositoryPort.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
        return joinTeam(employeeId, teamId);
    }

    public Team findByCode(String code) {
        String normalizedCode = normalizeTeamCode(code);
        return teamRepositoryPort.findByTeamCode(normalizedCode)
                .orElseThrow(() -> new TeamNotFoundException(normalizedCode));
    }

    private TeamMembership joinTeam(UUID employeeId, UUID teamId) {
        membershipRepositoryPort.findByTeamIdAndEmployeeId(teamId, employeeId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Employee " + employeeId + " is already a member of team " + teamId);
                });
        return membershipRepositoryPort.save(new TeamMembership(teamId, employeeId));
    }

    private String generateUniqueTeamCode() {
        for (int attempt = 0; attempt < MAX_TEAM_CODE_ATTEMPTS; attempt++) {
            String candidate = generateTeamCode();
            if (teamRepositoryPort.findByTeamCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique team code");
    }

    private String generateTeamCode() {
        StringBuilder code = new StringBuilder(TEAM_CODE_LENGTH);
        for (int i = 0; i < TEAM_CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(TEAM_CODE_CHARS.length());
            code.append(TEAM_CODE_CHARS.charAt(index));
        }
        return code.toString();
    }

    private String normalizeTeamCode(String teamCode) {
        if (teamCode == null || teamCode.isBlank()) {
            throw new IllegalArgumentException("teamCode must not be blank");
        }
        return teamCode.trim().toUpperCase(Locale.ROOT);
    }
}
