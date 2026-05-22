package com.psytrack.unformulieren.domain.exception;

public class InvalidQuestionOrderException extends IllegalArgumentException {

	public InvalidQuestionOrderException(String message) {
		super(message);
	}

	public InvalidQuestionOrderException(String message, Throwable cause) {
		super(message, cause);
	}
}
