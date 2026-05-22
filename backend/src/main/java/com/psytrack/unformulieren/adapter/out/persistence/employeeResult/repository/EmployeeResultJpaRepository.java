package com.psytrack.unformulieren.adapter.out.persistence.employeeResult.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.psytrack.unformulieren.adapter.out.persistence.employeeResult.entity.EmployeeResultJpaEntity;
public interface EmployeeResultJpaRepository extends JpaRepository<EmployeeResultJpaEntity, UUID> {

    Optional<EmployeeResultJpaEntity> findByEmployeeId(UUID employeeId);

    Optional<EmployeeResultJpaEntity> findByFormId(UUID formId);

    Optional<EmployeeResultJpaEntity> findByEmployeeIdAndFormId(UUID employeeId, UUID formId);
}
