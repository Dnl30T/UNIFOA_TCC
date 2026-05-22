package com.psytrack.unformulieren.adapter.out.persistence.question.mapper;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.psytrack.unformulieren.adapter.out.persistence.form.entity.QuestionStructureUdt;
import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.model.Question;

public class QuestionPersistenceMapper {

    public QuestionStructureUdt toUdt(Question domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        return new QuestionStructureUdt(
                domain.getId(),
                domain.getFormId(),
                domain.getText(),
                domain.getType().name(),
                domain.isRequired(),
                domain.getConfig(),
                domain.getOrder());
    }

    public Question toDomain(QuestionStructureUdt udt, UUID formId) {
        Objects.requireNonNull(udt, "udt must not be null");
        Map<String, String> config = udt.getConfig() == null ? Map.of() : udt.getConfig();
        return Question.reconstitute(
                udt.getId(),
                formId,
                udt.getText(),
                QuestionType.valueOf(udt.getType()),
                udt.isRequired(),
                config,
                udt.getDisplayOrder());
    }
}
