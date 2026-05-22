package com.psytrack.unformulieren.adapter.out.persistence.formResponse.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;
import com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity.FormResponseCassandraEntity;
import com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity.FormResponsePrimaryKey;

/**
 * Spring Data Cassandra repository for the {@code form_responses} table.
 * <p>
 * The primary query pattern — fetch all responses for a given
 * {@code (form_id, employee_id)} pair — maps directly to a partition scan.
 * The {@code @AllowFiltering} queries ({@code findByKeyEmployeeId},
 * {@code findByKeyQuestionId}) perform full-table scans and are acceptable
 * for the current scale of a TCC project.
 * </p>
 */
public interface FormResponseCassandraRepository
        extends CassandraRepository<FormResponseCassandraEntity, FormResponsePrimaryKey> {

    /** Returns the submission for a given form+employee pair (single-partition read). */
    Optional<FormResponseCassandraEntity> findByKeyFormIdAndKeyEmployeeId(UUID formId, UUID employeeId);

    /** Full-table scan filtered by employee — acceptable at TCC scale. */
    @AllowFiltering
    List<FormResponseCassandraEntity> findByKeyEmployeeId(UUID employeeId);

    /** Full-table scan filtered by form — acceptable at TCC scale. */
    @AllowFiltering
    List<FormResponseCassandraEntity> findByKeyFormId(UUID formId);
}
