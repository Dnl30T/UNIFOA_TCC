package com.psytrack.unformulieren.adapter.out.persistence.form.entity;

import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "forms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FormJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 400)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;

    private FormJpaEntity(UUID id, String title, String description, Status status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public static FormJpaEntity of(UUID id, String title, String description, Status status) {
        return new FormJpaEntity(id, title, description, status);
    }
}
