package com.psytrack.unformulieren.adapter.out.persistence.employeeResult.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.psytrack.unformulieren.adapter.out.persistence.employeeResult.entity.EmployeeResultJpaEntity;
import com.psytrack.unformulieren.adapter.out.persistence.employeeResult.mapper.EmployeeResultPersistenceMapper;
import com.psytrack.unformulieren.adapter.out.persistence.employeeResult.repository.EmployeeResultJpaRepository;
import com.psytrack.unformulieren.application.port.out.EmployeeResultRepositoryPort;
import com.psytrack.unformulieren.domain.model.EmployeeResult;

@Repository
public class EmployeeResultPersistenceAdapter implements EmployeeResultRepositoryPort {

    private final EmployeeResultJpaRepository repository;
    private final EmployeeResultPersistenceMapper mapper;

    public EmployeeResultPersistenceAdapter(EmployeeResultJpaRepository repository) {
        this.repository = repository;
        this.mapper = new EmployeeResultPersistenceMapper();
    }

    @Override
    public EmployeeResult save(EmployeeResult employeeResult) {
        EmployeeResultJpaEntity saved = repository.save(mapper.toJpa(employeeResult));
        return mapper.toDomain(saved);
    }

    @Override
    public List<EmployeeResult> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<EmployeeResult> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<EmployeeResult> findByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeId(employeeId).map(mapper::toDomain);
    }

    @Override
    public Optional<EmployeeResult> findByFormId(UUID formId) {
        return repository.findByFormId(formId).map(mapper::toDomain);
    }

    @Override
    public List<EmployeeResult> findAllByFormId(UUID formId) {
        return repository.findAllByFormId(formId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<EmployeeResult> findByEmployeeIdAndFormId(UUID employeeId, UUID formId) {
        return repository.findByEmployeeIdAndFormId(employeeId, formId).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
