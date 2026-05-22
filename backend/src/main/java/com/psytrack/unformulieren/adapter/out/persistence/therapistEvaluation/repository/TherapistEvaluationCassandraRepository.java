package com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.repository;

import java.util.Optional;
import org.springframework.data.cassandra.repository.CassandraRepository;
import com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.entity.TherapistEvaluationEntity;
import com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.entity.TherapistEvaluationPrimaryKey;

public interface TherapistEvaluationCassandraRepository
        extends CassandraRepository<TherapistEvaluationEntity, TherapistEvaluationPrimaryKey> {

    Optional<TherapistEvaluationEntity> findByKeyFormIdAndKeyEmployeeId(
            java.util.UUID formId, java.util.UUID employeeId);
}
