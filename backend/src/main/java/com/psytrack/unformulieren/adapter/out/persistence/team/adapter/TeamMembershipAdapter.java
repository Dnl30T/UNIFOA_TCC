package com.psytrack.unformulieren.adapter.out.persistence.team.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.psytrack.unformulieren.adapter.out.persistence.team.mapper.TeamMembershipPersistenceMapper;
import com.psytrack.unformulieren.adapter.out.persistence.team.repository.TeamMembershipJpaRepository;
import com.psytrack.unformulieren.application.port.out.TeamMembershipRepositoryPort;
import com.psytrack.unformulieren.domain.model.TeamMembership;

@Repository
public class TeamMembershipAdapter implements TeamMembershipRepositoryPort {

    private final TeamMembershipJpaRepository jpaRepository;
    private final TeamMembershipPersistenceMapper mapper;

    public TeamMembershipAdapter(TeamMembershipJpaRepository jpaRepository,
                                 TeamMembershipPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public TeamMembership save(TeamMembership membership) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(membership)));
    }

    @Override
    public Optional<TeamMembership> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TeamMembership> findByTeamId(UUID teamId) {
        return jpaRepository.findByTeamId(teamId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TeamMembership> findByEmployeeId(UUID employeeId) {
        return jpaRepository.findByEmployeeId(employeeId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TeamMembership> findByTeamIdAndEmployeeId(UUID teamId, UUID employeeId) {
        return jpaRepository.findByTeamIdAndEmployeeId(teamId, employeeId)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByTeamIdAndEmployeeId(UUID teamId, UUID employeeId) {
        jpaRepository.deleteByTeamIdAndEmployeeId(teamId, employeeId);
    }
}
