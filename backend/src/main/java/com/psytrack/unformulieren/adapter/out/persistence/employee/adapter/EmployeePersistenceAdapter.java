package com.psytrack.unformulieren.adapter.out.persistence.employee.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.psytrack.unformulieren.adapter.out.persistence.employee.entity.EmployeeJpaEntity;
import com.psytrack.unformulieren.adapter.out.persistence.employee.mapper.EmployeePersistenceMapper;
import com.psytrack.unformulieren.adapter.out.persistence.employee.repository.EmployeeJpaRepository;
import com.psytrack.unformulieren.application.port.out.EmployeeRepositoryPort;
import com.psytrack.unformulieren.domain.model.Employee;

@Repository
public class EmployeePersistenceAdapter implements EmployeeRepositoryPort {

    private final EmployeeJpaRepository repository;
    private final EmployeePersistenceMapper mapper;

    public EmployeePersistenceAdapter(EmployeeJpaRepository repository) {
        this.repository = repository;
        this.mapper = new EmployeePersistenceMapper();
    }

    @Override
    public Employee save(Employee employee) {
        EmployeeJpaEntity saved = repository.save(mapper.toJpa(employee));
        return mapper.toDomain(saved);
    }

    @Override
    public List<Employee> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Employee> findByTeamId(UUID teamId) {
        return repository.findByTeamId(teamId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Employee> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Employee> findByAppUserId(UUID appUserId) {
        return repository.findByAppUserId(appUserId).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
