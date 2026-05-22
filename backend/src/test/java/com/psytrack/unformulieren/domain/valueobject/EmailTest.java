package com.psytrack.unformulieren.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void validEmail_isAccepted() {
        Email email = new Email("user@example.com");
        assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    void email_isNormalized_toLowercase() {
        Email email = new Email("User@EXAMPLE.COM");
        assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    void email_isTrimmed() {
        Email email = new Email("  user@example.com  ");
        assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"notanemail", "missing@", "@nodomain", "no spaces@here.com"})
    void invalidEmail_throwsIllegalArgumentException(String invalid) {
        assertThatThrownBy(() -> new Email(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullEmail_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyEmail_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
