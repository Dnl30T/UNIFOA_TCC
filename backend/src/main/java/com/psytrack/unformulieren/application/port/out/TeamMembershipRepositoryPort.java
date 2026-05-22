package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.model.TeamMembership;

public interface TeamMembershipRepositoryPort {

    TeamMembership save(TeamMembership membership);

    Optional<TeamMembership> findById(UUID id);

    List<TeamMembership> findByTeamId(UUID teamId);

    List<TeamMembership> findByEmployeeId(UUID employeeId);

    Optional<TeamMembership> findByTeamIdAndEmployeeId(UUID teamId, UUID employeeId);

    void deleteById(UUID id);

    void deleteByTeamIdAndEmployeeId(UUID teamId, UUID employeeId);
}
