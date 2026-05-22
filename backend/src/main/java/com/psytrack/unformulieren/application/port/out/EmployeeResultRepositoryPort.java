package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.model.EmployeeResult;

public interface EmployeeResultRepositoryPort {

    EmployeeResult save(EmployeeResult employeeResult);

    List<EmployeeResult> findAll();

    Optional<EmployeeResult> findById(UUID id);

    Optional<EmployeeResult> findByEmployeeId(UUID employeeId);

    Optional<EmployeeResult> findByFormId(UUID formId);

    Optional<EmployeeResult> findByEmployeeIdAndFormId(UUID employeeId, UUID formId);

    void deleteById(UUID id);
}
