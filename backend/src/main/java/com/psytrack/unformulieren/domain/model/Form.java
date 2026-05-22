package com.psytrack.unformulieren.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.psytrack.unformulieren.domain.validation.DomainValidations;

import com.psytrack.unformulieren.domain.enums.Status;

public class Form {

    private final UUID id;

    private final String title;

    private final String description;

    private final Status status;

    /** Username of the counselor who created this form. */
    private final String createdBy;

    /** Teams to which this form has been distributed. Empty until published. */
    private final List<UUID> teamIds;

    private final List<Question> questions;

    public Form(String title, String description, Status status) {
        this(UUID.randomUUID(), title, description, status, null, List.of(), List.of());
    }

    public Form(String title, String description, Status status, String createdBy) {
        this(UUID.randomUUID(), title, description, status, createdBy, List.of(), List.of());
    }

    public Form(String title, String description, Status status, List<Question> questions) {
        this(UUID.randomUUID(), title, description, status, null, List.of(), questions);
    }

    private Form(UUID id, String title, String description, Status status,
                 String createdBy, List<UUID> teamIds, List<Question> questions) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = DomainValidations.requireNonBlank(title, "title");
        this.description = description == null ? "" : description.trim();
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdBy = createdBy;
        this.teamIds = new ArrayList<>(teamIds == null ? List.of() : teamIds);
        this.questions = new ArrayList<>(Objects.requireNonNull(questions, "questions must not be null"));
    }

    public static Form reconstitute(UUID id, String title, String description, Status status) {
        return new Form(id, title, description, status, null, List.of(), List.of());
    }

    public static Form reconstitute(UUID id, String title, String description, Status status, List<Question> questions) {
        return new Form(id, title, description, status, null, List.of(), questions);
    }

    public static Form reconstitute(UUID id, String title, String description, Status status,
                                    String createdBy, List<UUID> teamIds, List<Question> questions) {
        return new Form(id, title, description, status, createdBy,
                teamIds == null ? List.of() : teamIds, questions);
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public List<UUID> getTeamIds() {
        return Collections.unmodifiableList(teamIds);
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public void addQuestion(Question question) {
        questions.add(Objects.requireNonNull(question, "question must not be null"));
    }

}
