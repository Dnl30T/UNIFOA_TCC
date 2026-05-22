package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.Status;
import com.psytrack.unformulieren.domain.model.Form;

public interface FormRepositoryPort {

    Form save(Form form);

    List<Form> findAll();

    Optional<Form> findById(UUID id);

    Optional<Form> findByTitle(String title);

    List<Form> findByStatus(Status status);

    List<Form> findByCreatedBy(String createdBy);

    void deleteById(UUID id);
}
