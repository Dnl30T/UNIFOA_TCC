package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.model.Employee;

public interface EmployeeRepositoryPort {

    Employee save(Employee employee);

    List<Employee> findAll();

    List<Employee> findByTeamId(UUID teamId);

    Optional<Employee> findById(UUID id);

    Optional<Employee> findByAppUserId(UUID appUserId);

    void deleteById(UUID id);
}
