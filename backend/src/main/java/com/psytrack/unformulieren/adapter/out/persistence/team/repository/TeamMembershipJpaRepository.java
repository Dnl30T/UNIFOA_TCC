package com.psytrack.unformulieren.adapter.out.persistence.team.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psytrack.unformulieren.adapter.out.persistence.team.entity.TeamMembershipJpaEntity;

public interface TeamMembershipJpaRepository extends JpaRepository<TeamMembershipJpaEntity, UUID> {

    List<TeamMembershipJpaEntity> findByTeamId(UUID teamId);

    List<TeamMembershipJpaEntity> findByEmployeeId(UUID employeeId);

    Optional<TeamMembershipJpaEntity> findByTeamIdAndEmployeeId(UUID teamId, UUID employeeId);

    void deleteByTeamIdAndEmployeeId(UUID teamId, UUID employeeId);
}
