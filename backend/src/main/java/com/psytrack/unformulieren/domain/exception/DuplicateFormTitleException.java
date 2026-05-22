package com.psytrack.unformulieren.domain.exception;

public class DuplicateFormTitleException extends RuntimeException {

    public DuplicateFormTitleException(String title) {
        super("A form with title '" + title + "' already exists");
    }
}
