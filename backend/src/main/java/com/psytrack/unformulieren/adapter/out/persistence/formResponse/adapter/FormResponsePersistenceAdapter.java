package com.psytrack.unformulieren.adapter.out.persistence.formResponse.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity.FormResponsePrimaryKey;
import com.psytrack.unformulieren.adapter.out.persistence.formResponse.mapper.FormResponsePersistenceMapper;
import com.psytrack.unformulieren.adapter.out.persistence.formResponse.repository.FormResponseCassandraRepository;
import com.psytrack.unformulieren.application.port.out.FormResponseRepositoryPort;
import com.psytrack.unformulieren.domain.model.FormResponse;

@Repository
public class FormResponsePersistenceAdapter implements FormResponseRepositoryPort {

    private final FormResponseCassandraRepository repository;
    private final FormResponsePersistenceMapper mapper;

    public FormResponsePersistenceAdapter(FormResponseCassandraRepository repository) {
        this.repository = repository;
        this.mapper = new FormResponsePersistenceMapper();
    }

    @Override
    public FormResponse save(FormResponse formResponse) {
        return mapper.toDomain(repository.save(mapper.toCassandra(formResponse)));
    }

    @Override
    public List<FormResponse> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<FormResponse> findByFormIdAndEmployeeId(UUID formId, UUID employeeId) {
        return repository.findByKeyFormIdAndKeyEmployeeId(formId, employeeId).map(mapper::toDomain);
    }

    @Override
    public List<FormResponse> findByEmployeeId(UUID employeeId) {
        return repository.findByKeyEmployeeId(employeeId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<FormResponse> findByFormId(UUID formId) {
        return repository.findByKeyFormId(formId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByFormId(UUID formId) {
        return !repository.findByKeyFormId(formId).isEmpty();
    }

    @Override
    public boolean existsByFormIdAndEmployeeId(UUID formId, UUID employeeId) {
        return repository.existsById(new FormResponsePrimaryKey(formId, employeeId));
    }

    @Override
    public void deleteByFormIdAndEmployeeId(UUID formId, UUID employeeId) {
        repository.deleteById(new FormResponsePrimaryKey(formId, employeeId));
    }
}
