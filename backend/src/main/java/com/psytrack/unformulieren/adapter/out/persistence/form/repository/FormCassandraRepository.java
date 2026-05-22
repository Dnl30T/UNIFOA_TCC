package com.psytrack.unformulieren.adapter.out.persistence.form.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.cassandra.repository.CassandraRepository;
import com.psytrack.unformulieren.adapter.out.persistence.form.entity.FormCassandraEntity;

/**
 * Spring Data Cassandra repository for the {@code forms} table.
 * <p>
 * Queries by {@code title} are supported via the secondary index defined in
 * {@code schema.cql} ({@code CREATE INDEX forms_title_idx ON forms (title)}).
 * </p>
 */
public interface FormCassandraRepository extends CassandraRepository<FormCassandraEntity, UUID> {

    Optional<FormCassandraEntity> findByTitle(String title);

    List<FormCassandraEntity> findByCreatedBy(String createdBy);
}
