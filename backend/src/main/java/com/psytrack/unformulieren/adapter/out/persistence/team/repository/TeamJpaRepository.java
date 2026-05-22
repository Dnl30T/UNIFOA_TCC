package com.psytrack.unformulieren.adapter.out.persistence.team.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psytrack.unformulieren.adapter.out.persistence.team.entity.TeamJpaEntity;

public interface TeamJpaRepository extends JpaRepository<TeamJpaEntity, UUID> {

    Optional<TeamJpaEntity> findByName(String name);

    Optional<TeamJpaEntity> findByTeamCode(String teamCode);

    Optional<TeamJpaEntity> findByManagerId(UUID managerId);

    List<TeamJpaEntity> findByCounselorId(UUID counselorId);
}
