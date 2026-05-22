package com.psytrack.unformulieren.adapter.out.persistence.teamResult.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.psytrack.unformulieren.adapter.out.persistence.teamResult.entity.TeamResultJpaEntity;
import com.psytrack.unformulieren.adapter.out.persistence.teamResult.mapper.TeamResultPersistenceMapper;
import com.psytrack.unformulieren.adapter.out.persistence.teamResult.repository.TeamResultJpaRepository;
import com.psytrack.unformulieren.application.port.out.TeamResultRepositoryPort;
import com.psytrack.unformulieren.domain.model.TeamResult;

@Repository
public class TeamResultPersistenceAdapter implements TeamResultRepositoryPort {

    private final TeamResultJpaRepository repository;
    private final TeamResultPersistenceMapper mapper;

    public TeamResultPersistenceAdapter(TeamResultJpaRepository repository) {
        this.repository = repository;
        this.mapper = new TeamResultPersistenceMapper();
    }

    @Override
    public TeamResult save(TeamResult teamResult) {
        TeamResultJpaEntity saved = repository.save(mapper.toJpa(teamResult));
        return mapper.toDomain(saved);
    }

    @Override
    public List<TeamResult> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<TeamResult> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TeamResult> findByTeamId(UUID teamId) {
        return repository.findByTeamId(teamId).map(mapper::toDomain);
    }

    @Override
    public Optional<TeamResult> findByFormId(UUID formId) {
        return repository.findByFormId(formId).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
