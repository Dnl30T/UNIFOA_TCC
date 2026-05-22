package com.psytrack.unformulieren.adapter.out.persistence.employee.mapper;

import java.util.Objects;

import com.psytrack.unformulieren.adapter.out.persistence.employee.entity.EmployeeJpaEntity;
import com.psytrack.unformulieren.domain.model.Employee;

public class EmployeePersistenceMapper {

    public EmployeeJpaEntity toJpa(Employee domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        return EmployeeJpaEntity.of(
                domain.getId(),
                domain.getName(),
                domain.getAppUserId(),
                domain.getTeamId(),
                domain.getStatus());
    }

    public Employee toDomain(EmployeeJpaEntity jpa) {
        Objects.requireNonNull(jpa, "jpa must not be null");
        return Employee.reconstitute(
                jpa.getId(),
                jpa.getName(),
                jpa.getAppUserId(),
                jpa.getTeamId(),
                jpa.getStatus());
    }
}
