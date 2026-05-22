package com.psytrack.unformulieren.domain.exception;

public class DuplicateEmployeeEmailException extends RuntimeException {

    public DuplicateEmployeeEmailException(String email) {
        super("Employee email is already in use: " + email);
    }
}