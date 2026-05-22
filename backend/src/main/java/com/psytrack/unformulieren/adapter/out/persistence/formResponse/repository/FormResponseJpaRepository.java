package com.psytrack.unformulieren.adapter.out.persistence.formResponse.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity.FormResponseJpaEntity;

public interface FormResponseJpaRepository extends JpaRepository<FormResponseJpaEntity, UUID> {

    List<FormResponseJpaEntity> findByQuestionId(UUID questionId);

    List<FormResponseJpaEntity> findByEmployeeId(UUID employeeId);
}
