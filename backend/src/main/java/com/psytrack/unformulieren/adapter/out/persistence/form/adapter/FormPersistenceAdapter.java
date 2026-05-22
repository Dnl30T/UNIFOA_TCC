package com.psytrack.unformulieren.adapter.out.persistence.form.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import com.psytrack.unformulieren.adapter.out.persistence.form.mapper.FormPersistenceMapper;
import com.psytrack.unformulieren.adapter.out.persistence.form.repository.FormCassandraRepository;
import com.psytrack.unformulieren.application.port.out.FormRepositoryPort;
import com.psytrack.unformulieren.domain.enums.Status;
import com.psytrack.unformulieren.domain.model.Form;

/**
 * Cassandra/ScyllaDB implementation of {@link FormRepositoryPort}.
 * <p>
 * Reads and writes the denormalised {@code forms} table so that a single
 * partition lookup returns the complete form including all embedded questions.
 * </p>
 */
@Repository
public class FormPersistenceAdapter implements FormRepositoryPort {

    private final FormCassandraRepository repository;
    private final FormPersistenceMapper mapper;

    public FormPersistenceAdapter(FormCassandraRepository repository) {
        this.repository = repository;
        this.mapper = new FormPersistenceMapper();
    }

    @Override
    public Form save(Form form) {
        return mapper.toDomain(repository.save(mapper.toCassandra(form)));
    }

    @Override
    public List<Form> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Form> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Form> findByTitle(String title) {
        return repository.findByTitle(title).map(mapper::toDomain);
    }

    @Override
    public List<Form> findByStatus(Status status) {
        return repository.findAll().stream()
                .filter(e -> status.name().equals(e.getStatus()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Form> findByCreatedBy(String createdBy) {
        return repository.findAll().stream()
                .filter(e -> createdBy.equals(e.getCreatedBy()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
