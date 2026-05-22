package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class EmployeeResultNotFoundException extends RuntimeException {

    public EmployeeResultNotFoundException(UUID id) {
        super("EmployeeResult not found with id: " + id);
    }
}
