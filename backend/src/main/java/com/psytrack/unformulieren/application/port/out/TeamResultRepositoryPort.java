package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.model.TeamResult;

public interface TeamResultRepositoryPort {

    TeamResult save(TeamResult teamResult);

    List<TeamResult> findAll();

    Optional<TeamResult> findById(UUID id);

    Optional<TeamResult> findByTeamId(UUID teamId);

    Optional<TeamResult> findByFormId(UUID formId);

    void deleteById(UUID id);
}
