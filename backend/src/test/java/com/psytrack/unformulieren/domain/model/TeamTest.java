package com.psytrack.unformulieren.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamTest {

    private final UUID managerId = UUID.randomUUID();

    @Test
    void constructor_setsFields() {
        Team team = new Team("Alpha", managerId, "ALPHA-01");

        assertThat(team.getId()).isNotNull();
        assertThat(team.getName()).isEqualTo("Alpha");
        assertThat(team.getManagerId()).isEqualTo(managerId);
        assertThat(team.getTeamCode()).isEqualTo("ALPHA-01");
        assertThat(team.getCounselorId()).isNull();
        assertThat(team.hasCounselor()).isFalse();
    }

    @Test
    void constructor_rejectsBlankName() {
        assertThatThrownBy(() -> new Team("  ", managerId, "CODE1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void constructor_rejectsNullManagerId() {
        assertThatThrownBy(() -> new Team("Alpha", null, "CODE1"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void assignCounselor_setsCounselorId() {
        Team team = new Team("Beta", managerId, "BETA-01");
        UUID counselorId = UUID.randomUUID();

        team.assignCounselor(counselorId);

        assertThat(team.getCounselorId()).isEqualTo(counselorId);
        assertThat(team.hasCounselor()).isTrue();
    }

    @Test
    void assignCounselor_rejectsNull() {
        Team team = new Team("Beta", managerId, "BETA-01");
        assertThatThrownBy(() -> team.assignCounselor(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void changeManager_updatesManagerId() {
        Team team = new Team("Gamma", managerId, "GAM-01");
        UUID newManagerId = UUID.randomUUID();

        team.changeManager(newManagerId);

        assertThat(team.getManagerId()).isEqualTo(newManagerId);
    }

    @Test
    void reconstitute_rebuildsAllFields() {
        UUID id = UUID.randomUUID();
        UUID counselorId = UUID.randomUUID();
        java.time.Instant now = java.time.Instant.now();

        Team team = Team.reconstitute(id, "Delta", managerId, counselorId, "DELTA-01", now, now);

        assertThat(team.getId()).isEqualTo(id);
        assertThat(team.getName()).isEqualTo("Delta");
        assertThat(team.getCounselorId()).isEqualTo(counselorId);
        assertThat(team.getTeamCode()).isEqualTo("DELTA-01");
    }
}
