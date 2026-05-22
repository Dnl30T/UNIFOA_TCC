package com.psytrack.unformulieren.adapter.out.persistence.formResponse.mapper;

import java.util.Collections;
import java.util.Objects;

import com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity.FormResponseCassandraEntity;
import com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity.FormResponsePrimaryKey;
import com.psytrack.unformulieren.domain.enums.FormResponseStatus;
import com.psytrack.unformulieren.domain.model.FormResponse;

public class FormResponsePersistenceMapper {

    public FormResponseCassandraEntity toCassandra(FormResponse domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        return new FormResponseCassandraEntity(
                new FormResponsePrimaryKey(domain.getFormId(), domain.getEmployeeId()),
                domain.getId(),
                domain.getAnswers(),
                domain.getTextAnswers(),
            domain.getSubmittedAt(),
            domain.getStatus().name(),
            domain.getClosedAt());
    }

    public FormResponse toDomain(FormResponseCassandraEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return FormResponse.reconstitute(
                entity.getSubmissionId(),
                entity.getKey().getFormId(),
                entity.getKey().getEmployeeId(),
                entity.getAnswers() != null ? entity.getAnswers() : Collections.emptyMap(),
                entity.getTextAnswers() != null ? entity.getTextAnswers() : Collections.emptyMap(),
            entity.getSubmittedAt(),
            entity.getResponseStatus() == null
                ? FormResponseStatus.RESPONDED
                : FormResponseStatus.valueOf(entity.getResponseStatus()),
            entity.getClosedAt());
    }
}
