package com.psytrack.unformulieren.domain.model;

import com.psytrack.unformulieren.domain.enums.EmployeeStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeTest {

    private final UUID appUserId = UUID.randomUUID();
    private final UUID teamId    = UUID.randomUUID();

    @Test
    void hire_createsActiveEmployee() {
        Employee employee = Employee.hire("Bob", appUserId, teamId);

        assertThat(employee.getId()).isNotNull();
        assertThat(employee.getName()).isEqualTo("Bob");
        assertThat(employee.getAppUserId()).isEqualTo(appUserId);
        assertThat(employee.getTeamId()).isEqualTo(teamId);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.isActive()).isTrue();
    }

    @Test
    void hire_rejectsBlankName() {
        assertThatThrownBy(() -> Employee.hire("", appUserId, teamId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hire_rejectsNullAppUserId() {
        assertThatThrownBy(() -> Employee.hire("Bob", null, teamId))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deactivate_changesStatus() {
        Employee employee = Employee.hire("Carol", appUserId, teamId);
        employee.deactivate();

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
        assertThat(employee.isActive()).isFalse();
    }

    @Test
    void activate_changesStatusBackToActive() {
        Employee employee = Employee.hire("Dave", appUserId, teamId);
        employee.deactivate();
        employee.activate();

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.isActive()).isTrue();
    }

    @Test
    void changeTeam_updatesTeamId() {
        Employee employee = Employee.hire("Eve", appUserId, teamId);
        UUID newTeamId = UUID.randomUUID();

        employee.changeTeam(newTeamId);

        assertThat(employee.getTeamId()).isEqualTo(newTeamId);
    }

    @Test
    void changeTeam_rejectsNull() {
        Employee employee = Employee.hire("Eve", appUserId, teamId);
        assertThatThrownBy(() -> employee.changeTeam(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reconstitute_rebuildsEmployee() {
        UUID id = UUID.randomUUID();
        Employee employee = Employee.reconstitute(id, "Frank", appUserId, teamId, EmployeeStatus.INACTIVE);

        assertThat(employee.getId()).isEqualTo(id);
        assertThat(employee.getName()).isEqualTo("Frank");
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
        assertThat(employee.isActive()).isFalse();
    }
}
