package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.model.Question;

public interface QuestionRepositoryPort {

    Question save(Question question);

    List<Question> findAll();

    Optional<Question> findById(UUID id);

    List<Question> findByType(QuestionType type);

    void deleteById(UUID id);
}
