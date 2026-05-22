package com.psytrack.unformulieren.adapter.out.persistence.form.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psytrack.unformulieren.adapter.out.persistence.form.entity.FormJpaEntity;
import com.psytrack.unformulieren.domain.enums.Status;

public interface FormJpaRepository extends JpaRepository<FormJpaEntity, UUID> {

    Optional<FormJpaEntity> findByTitle(String title);

    List<FormJpaEntity> findByStatus(Status status);
}
