package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.FormResponseStatus;

public class FormResponse {

    private final UUID id;
    private final UUID formId;
    private final UUID employeeId;
    private final Map<UUID, Integer> answers;
    private final Map<UUID, String> textAnswers;
    private final Instant submittedAt;
    private final FormResponseStatus status;
    private final Instant closedAt;

    public FormResponse(UUID formId, UUID employeeId,
            Map<UUID, Integer> answers, Map<UUID, String> textAnswers, Instant submittedAt) {
        this(UUID.randomUUID(), formId, employeeId, answers, textAnswers, submittedAt,
                FormResponseStatus.RESPONDED, null);
    }

    public FormResponse(UUID formId, UUID employeeId,
            Map<UUID, Integer> answers, Map<UUID, String> textAnswers, Instant submittedAt,
            FormResponseStatus status, Instant closedAt) {
        this(UUID.randomUUID(), formId, employeeId, answers, textAnswers, submittedAt, status, closedAt);
    }

    private FormResponse(UUID id, UUID formId, UUID employeeId,
            Map<UUID, Integer> answers, Map<UUID, String> textAnswers, Instant submittedAt,
            FormResponseStatus status, Instant closedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.formId = Objects.requireNonNull(formId, "formId must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.answers = Objects.requireNonNull(answers, "answers must not be null");
        this.textAnswers = textAnswers != null ? textAnswers : Collections.emptyMap();
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.closedAt = closedAt;
    }

    public static FormResponse reconstitute(UUID id, UUID formId, UUID employeeId,
            Map<UUID, Integer> answers, Map<UUID, String> textAnswers, Instant submittedAt) {
        return new FormResponse(id, formId, employeeId, answers, textAnswers, submittedAt,
                FormResponseStatus.RESPONDED, null);
    }

    public static FormResponse reconstitute(UUID id, UUID formId, UUID employeeId,
            Map<UUID, Integer> answers, Map<UUID, String> textAnswers, Instant submittedAt,
            FormResponseStatus status, Instant closedAt) {
        return new FormResponse(id, formId, employeeId, answers, textAnswers, submittedAt, status, closedAt);
    }

    public UUID getId() { return id; }
    public UUID getFormId() { return formId; }
    public UUID getEmployeeId() { return employeeId; }
    public Map<UUID, Integer> getAnswers() { return answers; }
    public Map<UUID, String> getTextAnswers() { return textAnswers; }
    public Instant getSubmittedAt() { return submittedAt; }
    public FormResponseStatus getStatus() { return status; }
    public Instant getClosedAt() { return closedAt; }
}
