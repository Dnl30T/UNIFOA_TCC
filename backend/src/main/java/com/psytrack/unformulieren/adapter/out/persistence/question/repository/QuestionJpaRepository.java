package com.psytrack.unformulieren.adapter.out.persistence.question.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psytrack.unformulieren.adapter.out.persistence.question.entity.QuestionJpaEntity;
import com.psytrack.unformulieren.domain.enums.QuestionType;

public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, UUID> {

    List<QuestionJpaEntity> findByType(QuestionType type);
}
