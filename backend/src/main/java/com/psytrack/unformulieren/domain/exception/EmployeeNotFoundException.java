package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(UUID employeeId) {
        super("Employee not found for id: " + employeeId);
    }
}