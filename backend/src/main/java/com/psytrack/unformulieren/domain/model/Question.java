package com.psytrack.unformulieren.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.exception.InvalidQuestionOrderException;

public class Question {

	private final UUID id;
	private final UUID formId;
	private final String text;
	private final QuestionType type;
	private final boolean required;
	private final Map<String, String> config;
	private final int order;

	public Question(UUID formId, String text, QuestionType type, boolean required, Map<String, String> config, int order) {
		this(UUID.randomUUID(), formId, text, type, required, config, order);
	}

	private Question(UUID id, UUID formId, String text, QuestionType type, boolean required, Map<String, String> config, int order) {
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("Question text must not be blank");
		}
		this.id = Objects.requireNonNull(id, "Question id must not be null");
		this.formId = Objects.requireNonNull(formId, "Question formId must not be null");
		this.text = text;
		this.type = Objects.requireNonNull(type, "Question type must not be null");
		this.required = required;
		this.config = Objects.requireNonNull(config, "Question config must not be null");
		try {
			this.order = Objects.checkIndex(order, Integer.MAX_VALUE);
		} catch (IndexOutOfBoundsException e) {
			throw new InvalidQuestionOrderException("Question order must be a non-negative integer", e);
		}
	}

	public static Question reconstitute(UUID id, UUID formId, String text, QuestionType type, boolean required,
			Map<String, String> config, int order) {
		return new Question(id, formId, text, type, required, config, order);
	}

	public UUID getId() {
		return id;
	}

	public UUID getFormId() {
		return formId;
	}

	public String getText() {
		return text;
	}

	public QuestionType getType() {
		return type;
	}

	public boolean isRequired() {
		return required;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public int getOrder() {
		return order;
	}
}
