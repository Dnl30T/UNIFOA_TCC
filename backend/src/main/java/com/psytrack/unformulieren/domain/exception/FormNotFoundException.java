package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class FormNotFoundException extends RuntimeException {

    public FormNotFoundException(UUID id) {
        super("Form not found with id: " + id);
    }
}
