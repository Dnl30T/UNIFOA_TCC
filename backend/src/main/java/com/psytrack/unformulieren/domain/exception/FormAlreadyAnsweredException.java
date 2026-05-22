package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class FormAlreadyAnsweredException extends RuntimeException {

    public FormAlreadyAnsweredException(UUID formId, UUID employeeId) {
        super("Employee " + employeeId + " has already submitted responses for form " + formId + ".");
    }
}
