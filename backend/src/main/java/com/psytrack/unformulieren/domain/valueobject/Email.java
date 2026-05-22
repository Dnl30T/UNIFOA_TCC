package com.psytrack.unformulieren.domain.valueobject;

import com.sanctionco.jmail.JMail;

public class Email {

    private final String value;

    public Email(String value) {
        this.value = normalizeAndValidate(value);
    }

    public String getValue() {
        return value;
    }

    private static String normalizeAndValidate(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return normalizedEmail;
    }

    private static boolean isValidEmail(String email) {
        return JMail.isValid(email);
    }

}
