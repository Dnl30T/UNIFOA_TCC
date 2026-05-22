package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class TeamResultNotFoundException extends RuntimeException {

    public TeamResultNotFoundException(UUID id) {
        super("TeamResult not found with id: " + id);
    }
}
