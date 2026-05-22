package com.psytrack.unformulieren.adapter.out.persistence.employeeResult.mapper;

import java.util.Objects;

import com.psytrack.unformulieren.adapter.out.persistence.employeeResult.entity.EmployeeResultJpaEntity;
import com.psytrack.unformulieren.domain.model.EmployeeResult;

public class EmployeeResultPersistenceMapper {

    public EmployeeResultJpaEntity toJpa(EmployeeResult domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        return EmployeeResultJpaEntity.of(
                domain.getId(),
                domain.getEmployeeId(),
                domain.getFormId(),
            domain.getHelperScore(),
            domain.getFinalScore(),
                domain.getRiskLevel(),
                domain.getCalculatedAt());
    }

    public EmployeeResult toDomain(EmployeeResultJpaEntity jpa) {
        Objects.requireNonNull(jpa, "jpa must not be null");
        return EmployeeResult.reconstitute(
                jpa.getId(),
                jpa.getEmployeeId(),
                jpa.getFormId(),
                jpa.getScore(),
            jpa.getFinalScore(),
                jpa.getRiskLevel(),
                jpa.getCalculatedAt());
    }
}
