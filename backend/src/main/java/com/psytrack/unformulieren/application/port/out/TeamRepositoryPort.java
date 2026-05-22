package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.model.Team;

public interface TeamRepositoryPort {

    Team save(Team team);

    List<Team> findAll();

    Optional<Team> findById(UUID id);

    Optional<Team> findByName(String name);

    Optional<Team> findByTeamCode(String teamCode);

    Optional<Team> findByManagerId(UUID managerId);

    List<Team> findByCounselorId(UUID counselorId);

    void deleteById(UUID id);
}
