package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class ManagerAlreadyHasTeamException extends RuntimeException {

    public ManagerAlreadyHasTeamException(UUID managerId) {
        super("Manager " + managerId + " already owns a team. Each manager can own at most one team.");
    }
}
