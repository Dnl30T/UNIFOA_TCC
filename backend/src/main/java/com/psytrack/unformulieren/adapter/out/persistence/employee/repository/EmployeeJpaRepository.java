package com.psytrack.unformulieren.adapter.out.persistence.employee.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.psytrack.unformulieren.adapter.out.persistence.employee.entity.EmployeeJpaEntity;
public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {

    Optional<EmployeeJpaEntity> findByAppUserId(UUID appUserId);

    List<EmployeeJpaEntity> findByTeamId(UUID teamId);
}
