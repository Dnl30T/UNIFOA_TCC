package com.psytrack.unformulieren.adapter.out.persistence.teamResult.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.psytrack.unformulieren.adapter.out.persistence.teamResult.entity.TeamResultJpaEntity;
public interface TeamResultJpaRepository extends JpaRepository<TeamResultJpaEntity, UUID> {

    Optional<TeamResultJpaEntity> findByTeamId(UUID teamId);

    Optional<TeamResultJpaEntity> findByFormId(UUID formId);
}
