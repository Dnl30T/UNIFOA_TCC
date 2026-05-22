package com.psytrack.unformulieren.adapter.out.persistence.question.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import com.psytrack.unformulieren.adapter.out.persistence.form.entity.FormCassandraEntity;
import com.psytrack.unformulieren.adapter.out.persistence.form.entity.QuestionStructureUdt;
import com.psytrack.unformulieren.adapter.out.persistence.form.repository.FormCassandraRepository;
import com.psytrack.unformulieren.adapter.out.persistence.question.mapper.QuestionPersistenceMapper;
import com.psytrack.unformulieren.application.port.out.QuestionRepositoryPort;
import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.exception.FormNotFoundException;
import com.psytrack.unformulieren.domain.exception.QuestionNotFoundException;
import com.psytrack.unformulieren.domain.model.Question;

/**
 * Cassandra/ScyllaDB implementation of {@link QuestionRepositoryPort}.
 * <p>
 * Questions are not stored in a separate table. Instead they live as UDT
 * elements inside the {@code questions} LIST of the parent {@code forms} row.
 * All mutation operations load the parent form, update the embedded list, and
 * persist the form back — a single-partition write.
 * </p>
 */
@Repository
public class QuestionPersistenceAdapter implements QuestionRepositoryPort {

    private final FormCassandraRepository formRepository;
    private final QuestionPersistenceMapper mapper;

    public QuestionPersistenceAdapter(FormCassandraRepository formRepository) {
        this.formRepository = formRepository;
        this.mapper = new QuestionPersistenceMapper();
    }

    @Override
    public Question save(Question question) {
        UUID formId = question.getFormId();
        FormCassandraEntity form = formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        List<QuestionStructureUdt> questions = new ArrayList<>(
                form.getQuestions() == null ? List.of() : form.getQuestions());

        // Remove stale version (update scenario)
        questions.removeIf(udt -> udt.getId().equals(question.getId()));
        questions.add(mapper.toUdt(question));

        form.setQuestions(questions);
        formRepository.save(form);
        return question;
    }

    @Override
    public List<Question> findAll() {
        return formRepository.findAll().stream()
                .filter(f -> f.getQuestions() != null)
                .flatMap(f -> f.getQuestions().stream()
                        .map(udt -> mapper.toDomain(udt, f.getFormId())))
                .toList();
    }

    @Override
    public Optional<Question> findById(UUID id) {
        return formRepository.findAll().stream()
                .filter(f -> f.getQuestions() != null)
                .flatMap(f -> f.getQuestions().stream()
                        .filter(udt -> udt.getId().equals(id))
                        .map(udt -> mapper.toDomain(udt, f.getFormId())))
                .findFirst();
    }

    @Override
    public List<Question> findByType(QuestionType type) {
        String typeName = type.name();
        return formRepository.findAll().stream()
                .filter(f -> f.getQuestions() != null)
                .flatMap(f -> f.getQuestions().stream()
                        .filter(udt -> typeName.equals(udt.getType()))
                        .map(udt -> mapper.toDomain(udt, f.getFormId())))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        formRepository.findAll().stream()
                .filter(f -> f.getQuestions() != null &&
                        f.getQuestions().stream().anyMatch(udt -> udt.getId().equals(id)))
                .findFirst()
                .ifPresentOrElse(form -> {
                    List<QuestionStructureUdt> updated = new ArrayList<>(form.getQuestions());
                    updated.removeIf(udt -> udt.getId().equals(id));
                    form.setQuestions(updated);
                    formRepository.save(form);
                }, () -> { throw new QuestionNotFoundException(id); });
    }
}
