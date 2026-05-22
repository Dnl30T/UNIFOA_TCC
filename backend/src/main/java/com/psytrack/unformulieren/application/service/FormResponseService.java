package com.psytrack.unformulieren.application.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.FormResponseRepositoryPort;
import com.psytrack.unformulieren.application.port.out.FormRepositoryPort;
import com.psytrack.unformulieren.domain.enums.FormResponseStatus;
import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.exception.FormAlreadyAnsweredException;
import com.psytrack.unformulieren.domain.exception.FormResponseNotFoundException;
import com.psytrack.unformulieren.domain.model.Question;
import com.psytrack.unformulieren.domain.model.FormResponse;

@Service
public class FormResponseService {

    private final FormResponseRepositoryPort formResponseRepositoryPort;
    private final EmployeeResultService employeeResultService;
    private final FormRepositoryPort formRepositoryPort;

    public FormResponseService(FormResponseRepositoryPort formResponseRepositoryPort,
                               EmployeeResultService employeeResultService,
                               FormRepositoryPort formRepositoryPort) {
        this.formResponseRepositoryPort = formResponseRepositoryPort;
        this.employeeResultService = employeeResultService;
        this.formRepositoryPort = formRepositoryPort;
    }

    public FormResponse submitBatch(UUID formId, UUID employeeId,
            Map<UUID, Integer> answers, Map<UUID, String> textAnswers, Instant submittedAt) {
        if (formResponseRepositoryPort.existsByFormIdAndEmployeeId(formId, employeeId)) {
            throw new FormAlreadyAnsweredException(formId, employeeId);
        }
        FormResponse saved = formResponseRepositoryPort.save(
                new FormResponse(formId, employeeId, answers, textAnswers, submittedAt));

        int helperScore = calculateHelperScore(formId, answers);
        employeeResultService.createOrUpdateHelperScore(employeeId, formId, helperScore, submittedAt);
        return saved;
    }

    public FormResponse markNoResponse(UUID formId, UUID employeeId, Instant closedAt) {
        return formResponseRepositoryPort.save(new FormResponse(
                formId,
                employeeId,
                Collections.emptyMap(),
                Collections.emptyMap(),
                closedAt,
                FormResponseStatus.NO_RESPONSE,
                closedAt));
    }

    public FormResponse getByFormAndEmployee(UUID formId, UUID employeeId) {
        return formResponseRepositoryPort.findByFormIdAndEmployeeId(formId, employeeId)
                .orElseThrow(() -> new FormResponseNotFoundException(formId));
    }

    public List<FormResponse> list() {
        return formResponseRepositoryPort.findAll();
    }

    public List<FormResponse> findByEmployeeId(UUID employeeId) {
        return formResponseRepositoryPort.findByEmployeeId(employeeId);
    }

    public List<FormResponse> findByFormId(UUID formId) {
        return formResponseRepositoryPort.findByFormId(formId);
    }

    public boolean hasSubmitted(UUID formId, UUID employeeId) {
        return formResponseRepositoryPort.findByFormIdAndEmployeeId(formId, employeeId)
                .map(response -> response.getStatus() == FormResponseStatus.RESPONDED)
                .orElse(false);
    }

    private int calculateHelperScore(UUID formId, Map<UUID, Integer> answers) {
        if (answers == null || answers.isEmpty()) {
            return 0;
        }

        Map<UUID, Question> questionById = new HashMap<>();
        formRepositoryPort.findById(formId)
                .ifPresent(form -> form.getQuestions().forEach(question -> questionById.put(question.getId(), question)));

        double weightedTotal = 0d;
        double totalWeight = 0d;

        for (Map.Entry<UUID, Integer> entry : answers.entrySet()) {
            Question question = questionById.get(entry.getKey());
            if (question == null) {
                continue;
            }
            double weight = parseWeight(question.getConfig());
            if (weight <= 0d) {
                continue;
            }
            double maxScoreForQuestion = inferMaxScore(question);
            double normalized = maxScoreForQuestion <= 0d
                    ? 0d
                    : (Math.max(0d, Math.min(entry.getValue(), maxScoreForQuestion)) / maxScoreForQuestion) * 100d;
            weightedTotal += normalized * weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0d) {
            return 0;
        }
        return (int) Math.round(weightedTotal / totalWeight);
    }

    private double parseWeight(Map<String, String> config) {
        if (config == null) {
            return 0d;
        }
        try {
            return Math.max(0d, Double.parseDouble(config.getOrDefault("weight", "0")));
        } catch (NumberFormatException exception) {
            return 0d;
        }
    }

    private double inferMaxScore(Question question) {
        Map<String, String> config = question.getConfig();
        if (config != null && config.containsKey("max")) {
            try {
                return Math.max(1d, Double.parseDouble(config.get("max")));
            } catch (NumberFormatException ignored) {
                // fallback to type heuristics below
            }
        }

        QuestionType type = question.getType();
        if (type == QuestionType.BOOLEAN) {
            return 1d;
        }
        if (type == QuestionType.LIKERT) {
            long options = config == null ? 0 : config.keySet().stream().filter(key -> key.startsWith("option_")).count();
            return Math.max(1d, options);
        }
        if (type == QuestionType.SINGLE_CHOICE) {
            long options = config == null ? 0 : config.keySet().stream().filter(key -> key.startsWith("option_")).count();
            return Math.max(1d, options - 1);
        }
        if (type == QuestionType.SLIDER || type == QuestionType.SCALE || type == QuestionType.NUMERIC) {
            return 100d;
        }
        return 100d;
    }

    public void delete(UUID formId, UUID employeeId) {
        if (!formResponseRepositoryPort.existsByFormIdAndEmployeeId(formId, employeeId)) {
            throw new FormResponseNotFoundException(formId);
        }
        formResponseRepositoryPort.deleteByFormIdAndEmployeeId(formId, employeeId);
    }
}
