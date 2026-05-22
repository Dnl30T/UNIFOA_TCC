package com.psytrack.unformulieren.adapter.out.persistence.form.mapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import com.psytrack.unformulieren.adapter.out.persistence.form.entity.FormCassandraEntity;
import com.psytrack.unformulieren.adapter.out.persistence.form.entity.QuestionStructureUdt;
import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.enums.Status;
import com.psytrack.unformulieren.domain.model.Form;
import com.psytrack.unformulieren.domain.model.Question;

public class FormPersistenceMapper {

    public FormCassandraEntity toCassandra(Form domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        List<QuestionStructureUdt> udts = domain.getQuestions().stream()
                .map(this::questionToUdt)
                .collect(Collectors.toList());
        return FormCassandraEntity.of(
                domain.getId(),
                domain.getTitle(),
                domain.getDescription(),
                domain.getStatus().name(),
                domain.getCreatedBy(),
                domain.getTeamIds(),
                udts);
    }

    public Form toDomain(FormCassandraEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        List<Question> questions = entity.getQuestions() == null
                ? List.of()
                : entity.getQuestions().stream()
                        .map(udt -> udtToQuestion(udt, entity.getFormId()))
                        .collect(Collectors.toList());
        return Form.reconstitute(
                entity.getFormId(),
                entity.getTitle(),
                entity.getDescription(),
                Status.valueOf(entity.getStatus()),
                entity.getCreatedBy(),
                entity.getTeamIds(),
                questions);
    }

    public QuestionStructureUdt questionToUdt(Question q) {
        return new QuestionStructureUdt(
                q.getId(),
                q.getFormId(),
                q.getText(),
                q.getType().name(),
                q.isRequired(),
                q.getConfig(),
                q.getOrder());
    }

    public Question udtToQuestion(QuestionStructureUdt udt, UUID formId) {
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
