package com.psytrack.unformulieren.domain.exception;

import java.util.UUID;

public class CounselorAlreadyHasTeamException extends RuntimeException {

    public CounselorAlreadyHasTeamException(UUID counselorId) {
        super("Counselor " + counselorId + " is already assigned to a team.");
    }
}
