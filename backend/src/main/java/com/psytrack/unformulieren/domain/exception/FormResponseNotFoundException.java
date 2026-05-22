package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class FormResponseNotFoundException extends RuntimeException {

    public FormResponseNotFoundException(UUID id) {
        super("FormResponse not found with id: " + id);
    }
}
