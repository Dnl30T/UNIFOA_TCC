package com.psytrack.unformulieren.adapter.out.persistence.team.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.psytrack.unformulieren.adapter.out.persistence.team.mapper.TeamPersistenceMapper;
import com.psytrack.unformulieren.adapter.out.persistence.team.repository.TeamJpaRepository;
import com.psytrack.unformulieren.application.port.out.TeamRepositoryPort;
import com.psytrack.unformulieren.domain.model.Team;

@Repository
public class TeamPersistenceAdapter implements TeamRepositoryPort {

    private final TeamJpaRepository repository;
    private final TeamPersistenceMapper mapper;

    public TeamPersistenceAdapter(TeamJpaRepository repository) {
        this.repository = repository;
        this.mapper = new TeamPersistenceMapper();
    }

    @Override
    public Team save(Team team) {
        return mapper.toDomain(repository.save(mapper.toJpa(team)));
    }

    @Override
    public List<Team> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Team> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public Optional<Team> findByTeamCode(String teamCode) {
        return repository.findByTeamCode(teamCode).map(mapper::toDomain);
    }

    @Override
    public Optional<Team> findByManagerId(UUID managerId) {
        return repository.findByManagerId(managerId).map(mapper::toDomain);
    }

    @Override
    public List<Team> findByCounselorId(UUID counselorId) {
        return repository.findByCounselorId(counselorId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
