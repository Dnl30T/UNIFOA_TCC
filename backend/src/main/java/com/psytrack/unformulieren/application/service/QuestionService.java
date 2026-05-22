package com.psytrack.unformulieren.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.QuestionRepositoryPort;
import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.exception.QuestionNotFoundException;
import com.psytrack.unformulieren.domain.model.Question;

@Service
public class QuestionService {

    private final QuestionRepositoryPort questionRepositoryPort;

    public QuestionService(QuestionRepositoryPort questionRepositoryPort) {
        this.questionRepositoryPort = questionRepositoryPort;
    }

    public Question create(UUID formId, String text, QuestionType type, boolean required,
            Map<String, String> config, int order) {
        validateFormWeights(formId, null, config);
        return questionRepositoryPort.save(new Question(formId, text, type, required, config, order));
    }

    public Question get(UUID id) {
        return questionRepositoryPort.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException(id));
    }

    public List<Question> list() {
        return questionRepositoryPort.findAll();
    }

    public List<Question> findByType(QuestionType type) {
        return questionRepositoryPort.findByType(type);
    }

    public Question update(UUID id, UUID formId, String text, QuestionType type, boolean required,
            Map<String, String> config, int order) {
        get(id);
        validateFormWeights(formId, id, config);
        return questionRepositoryPort.save(Question.reconstitute(id, formId, text, type, required, config, order));
    }

    private void validateFormWeights(UUID formId, UUID currentQuestionId, Map<String, String> incomingConfig) {
        double total = questionRepositoryPort.findAll().stream()
                .filter(question -> question.getFormId().equals(formId))
                .filter(question -> currentQuestionId == null || !question.getId().equals(currentQuestionId))
                .mapToDouble(question -> parseWeight(question.getConfig()))
                .sum();

        total += parseWeight(incomingConfig);
        if (total > 100d) {
            throw new IllegalArgumentException("Total question weights for a form cannot exceed 100");
        }
    }

    private double parseWeight(Map<String, String> config) {
        if (config == null) {
            return 0d;
        }
        try {
            return Math.max(0d, Double.parseDouble(config.getOrDefault("weight", "0")));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid question weight. Expected a numeric value.");
        }
    }

    public void delete(UUID id) {
        get(id);
        questionRepositoryPort.deleteById(id);
    }
}
