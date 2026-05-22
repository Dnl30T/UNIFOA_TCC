package com.psytrack.unformulieren.adapter.out.persistence.question.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.QuestionType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuestionType type;

    @Column(name = "required_flag", nullable = false)
    private boolean required;

    @ElementCollection
    @CollectionTable(name = "question_configs", joinColumns = @JoinColumn(name = "question_id"))
    @MapKeyColumn(name = "config_key", length = 120)
    @Column(name = "config_value", length = 600)
    private Map<String, String> config;

    @Column(name = "display_order", nullable = false)
    private int order;

    private QuestionJpaEntity(UUID id, String text, QuestionType type, boolean required, Map<String, String> config, int order) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.required = required;
        this.config = new LinkedHashMap<>(config);
        this.order = order;
    }

    public static QuestionJpaEntity of(UUID id, String text, QuestionType type, boolean required, Map<String, String> config, int order) {
        return new QuestionJpaEntity(id, text, type, required, config, order);
    }
}
