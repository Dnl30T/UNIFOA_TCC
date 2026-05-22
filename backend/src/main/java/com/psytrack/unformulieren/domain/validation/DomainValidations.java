package com.psytrack.unformulieren.domain.validation;

public class DomainValidations {

    private DomainValidations() {
        // Utility class
    }
    
    public static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

    

}
