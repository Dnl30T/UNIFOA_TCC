package com.psytrack.unformulieren.application.service;

import com.psytrack.unformulieren.application.port.out.TeamMembershipRepositoryPort;
import com.psytrack.unformulieren.application.port.out.TeamRepositoryPort;
import com.psytrack.unformulieren.domain.exception.DuplicateTeamNameException;
import com.psytrack.unformulieren.domain.exception.ManagerAlreadyHasTeamException;
import com.psytrack.unformulieren.domain.exception.TeamNotFoundException;
import com.psytrack.unformulieren.domain.model.Team;
import com.psytrack.unformulieren.domain.model.TeamMembership;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepositoryPort teamRepositoryPort;
    @Mock private TeamMembershipRepositoryPort membershipRepositoryPort;

    @InjectMocks private TeamService teamService;

    private final UUID managerId = UUID.randomUUID();

    private Team sampleTeam() {
        return new Team("Alpha", managerId, "ALPHA-01");
    }

    // ── registerTeam ─────────────────────────────────────────────────────────

    @Test
    void registerTeam_succeeds_whenManagerHasNoTeamAndNameIsUnique() {
        Team saved = sampleTeam();
        given(teamRepositoryPort.findByManagerId(managerId)).willReturn(Optional.empty());
        given(teamRepositoryPort.findByName("Alpha")).willReturn(Optional.empty());
        given(teamRepositoryPort.save(any(Team.class))).willReturn(saved);

        Team result = teamService.registerTeam("Alpha", managerId);

        assertThat(result).isNotNull();
        verify(teamRepositoryPort).save(any(Team.class));
    }

    @Test
    void registerTeam_throws_whenManagerAlreadyHasTeam() {
        given(teamRepositoryPort.findByManagerId(managerId)).willReturn(Optional.of(sampleTeam()));

        assertThatThrownBy(() -> teamService.registerTeam("Beta", managerId))
                .isInstanceOf(ManagerAlreadyHasTeamException.class);
    }

    @Test
    void registerTeam_throws_whenNameAlreadyTaken() {
        given(teamRepositoryPort.findByManagerId(managerId)).willReturn(Optional.empty());
        given(teamRepositoryPort.findByName("Alpha")).willReturn(Optional.of(sampleTeam()));

        assertThatThrownBy(() -> teamService.registerTeam("Alpha", managerId))
                .isInstanceOf(DuplicateTeamNameException.class);
    }

    // ── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_returnsTeam_whenFound() {
        Team team = sampleTeam();
        given(teamRepositoryPort.findById(team.getId())).willReturn(Optional.of(team));

        Team result = teamService.get(team.getId());

        assertThat(result).isEqualTo(team);
    }

    @Test
    void get_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(teamRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.get(id))
                .isInstanceOf(TeamNotFoundException.class);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAllTeams() {
        List<Team> teams = List.of(sampleTeam(), new Team("Beta", UUID.randomUUID(), "BETA-01"));
        given(teamRepositoryPort.findAll()).willReturn(teams);

        List<Team> result = teamService.list();

        assertThat(result).hasSize(2);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_succeeds_withNewUniqueName() {
        Team existing = sampleTeam();
        given(teamRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(teamRepositoryPort.findByName("Bravo")).willReturn(Optional.empty());
        given(teamRepositoryPort.save(any(Team.class))).willAnswer(inv -> inv.getArgument(0));

        Team updated = teamService.update(existing.getId(), "Bravo");

        assertThat(updated.getName()).isEqualTo("Bravo");
    }

    @Test
    void update_throws_whenNewNameTakenByDifferentTeam() {
        Team existing = sampleTeam();
        Team other = new Team("Bravo", UUID.randomUUID(), "BRAVO-01");
        given(teamRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(teamRepositoryPort.findByName("Bravo")).willReturn(Optional.of(other));

        assertThatThrownBy(() -> teamService.update(existing.getId(), "Bravo"))
                .isInstanceOf(DuplicateTeamNameException.class);
    }

    @Test
    void update_succeeds_withSameName() {
        Team existing = sampleTeam();
        given(teamRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(teamRepositoryPort.findByName("Alpha")).willReturn(Optional.of(existing));
        given(teamRepositoryPort.save(any(Team.class))).willAnswer(inv -> inv.getArgument(0));

        Team updated = teamService.update(existing.getId(), "Alpha");

        assertThat(updated.getName()).isEqualTo("Alpha");
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_succeeds_whenTeamExists() {
        Team team = sampleTeam();
        given(teamRepositoryPort.findById(team.getId())).willReturn(Optional.of(team));

        teamService.delete(team.getId());

        verify(teamRepositoryPort).deleteById(team.getId());
    }

    @Test
    void delete_throws_whenTeamNotFound() {
        UUID id = UUID.randomUUID();
        given(teamRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.delete(id))
                .isInstanceOf(TeamNotFoundException.class);
    }

    // ── assignCounselor ──────────────────────────────────────────────────────

    @Test
    void assignCounselor_setsCounselorAndSaves() {
        Team team = sampleTeam();
        UUID counselorId = UUID.randomUUID();
        given(teamRepositoryPort.findById(team.getId())).willReturn(Optional.of(team));
        given(teamRepositoryPort.save(team)).willReturn(team);

        Team result = teamService.assignCounselor(team.getId(), counselorId);

        assertThat(result.getCounselorId()).isEqualTo(counselorId);
    }

    // ── joinByCode ───────────────────────────────────────────────────────────

    @Test
    void joinByCode_succeeds_whenCodeExists() {
        Team team = sampleTeam();
        UUID employeeId = UUID.randomUUID();
        TeamMembership membership = new TeamMembership(team.getId(), employeeId);
        given(teamRepositoryPort.findByTeamCode("ALPHA-01")).willReturn(Optional.of(team));
        given(membershipRepositoryPort.findByTeamIdAndEmployeeId(team.getId(), employeeId))
                .willReturn(Optional.empty());
        given(membershipRepositoryPort.save(any(TeamMembership.class))).willReturn(membership);

        TeamMembership result = teamService.joinByCode(employeeId, "alpha-01");

        assertThat(result).isNotNull();
        verify(membershipRepositoryPort).save(any(TeamMembership.class));
    }

    @Test
    void joinByCode_throws_whenCodeNotFound() {
        given(teamRepositoryPort.findByTeamCode("NOPE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.joinByCode(UUID.randomUUID(), "NOPE"))
                .isInstanceOf(TeamNotFoundException.class);
    }
}
