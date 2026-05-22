package com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "form_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FormResponseJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private int value;

    @Column(name = "response_timestamp", nullable = false)
    private Instant responseTimestamp;

    private FormResponseJpaEntity(UUID id, UUID questionId, UUID employeeId, int value, Instant responseTimestamp) {
        this.id = id;
        this.questionId = questionId;
        this.employeeId = employeeId;
        this.value = value;
        this.responseTimestamp = responseTimestamp;
    }

    public static FormResponseJpaEntity of(UUID id, UUID questionId, UUID employeeId, int value, Instant responseTimestamp) {
        return new FormResponseJpaEntity(id, questionId, employeeId, value, responseTimestamp);
    }
}
