package com.psytrack.unformulieren.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.EmployeeRepositoryPort;
import com.psytrack.unformulieren.domain.enums.EmployeeStatus;
import com.psytrack.unformulieren.domain.exception.EmployeeNotFoundException;
import com.psytrack.unformulieren.domain.model.Employee;

@Service
public class EmployeeService {

    private final EmployeeRepositoryPort employeeRepositoryPort;

    public EmployeeService(EmployeeRepositoryPort employeeRepositoryPort) {
        this.employeeRepositoryPort = employeeRepositoryPort;
    }

    public Employee hireEmployee(String name, UUID appUserId, UUID teamId) {
        return employeeRepositoryPort.save(Employee.hire(name, appUserId, teamId));
    }

    public Employee get(UUID id) {
        return employeeRepositoryPort.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    public List<Employee> list() {
        return employeeRepositoryPort.findAll();
    }

    public List<Employee> listByTeam(UUID teamId) {
        return employeeRepositoryPort.findByTeamId(teamId);
    }

    public Employee findByAppUserId(UUID appUserId) {
        return employeeRepositoryPort.findByAppUserId(appUserId).orElse(null);
    }

    public Employee update(UUID id, String name, UUID teamId, EmployeeStatus status) {
        Employee existing = get(id);
        return employeeRepositoryPort.save(Employee.reconstitute(
                existing.getId(),
                name,
                existing.getAppUserId(),
                teamId,
                status == null ? existing.getStatus() : status));
    }

    public void delete(UUID id) {
        get(id);
        employeeRepositoryPort.deleteById(id);
    }
}
