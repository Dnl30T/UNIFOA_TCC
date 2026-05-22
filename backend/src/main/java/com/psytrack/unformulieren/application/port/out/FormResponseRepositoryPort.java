package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.model.FormResponse;

public interface FormResponseRepositoryPort {

    FormResponse save(FormResponse formResponse);

    List<FormResponse> findAll();

    Optional<FormResponse> findByFormIdAndEmployeeId(UUID formId, UUID employeeId);

    List<FormResponse> findByEmployeeId(UUID employeeId);

    List<FormResponse> findByFormId(UUID formId);

    boolean existsByFormId(UUID formId);

    boolean existsByFormIdAndEmployeeId(UUID formId, UUID employeeId);

    void deleteByFormIdAndEmployeeId(UUID formId, UUID employeeId);
}
