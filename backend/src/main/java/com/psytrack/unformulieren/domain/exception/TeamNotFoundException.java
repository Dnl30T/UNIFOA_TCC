package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(UUID id) {
        super("Team not found with id: " + id);
    }

    public TeamNotFoundException(String identifier) {
        super("Team not found: " + identifier);
    }
}
