package com.psytrack.unformulieren.domain.exception;

public class DuplicateTeamNameException extends RuntimeException {

    public DuplicateTeamNameException(String name) {
        super("A team with name '" + name + "' already exists");
    }
}
