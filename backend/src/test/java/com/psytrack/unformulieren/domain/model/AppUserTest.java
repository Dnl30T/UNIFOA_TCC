package com.psytrack.unformulieren.domain.model;

import com.psytrack.unformulieren.domain.enums.UserRole;
import com.psytrack.unformulieren.domain.valueobject.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppUserTest {

    private static final String USERNAME  = "alice";
    private static final String HASH      = "$2a$10$hashedpassword";
    private static final Email  EMAIL     = new Email("alice@example.com");
    private static final UserRole ROLE    = UserRole.EMPLOYEE;

    @Test
    void constructor_setsAllFields() {
        AppUser user = new AppUser(USERNAME, HASH, EMAIL, ROLE, true);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo(USERNAME);
        assertThat(user.getPasswordHash()).isEqualTo(HASH);
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getRole()).isEqualTo(ROLE);
    }

    @Test
    void constructor_rejectsConsentFalse() {
        assertThatThrownBy(() -> new AppUser(USERNAME, HASH, EMAIL, ROLE, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consent");
    }

    @Test
    void constructor_rejectsBlankUsername() {
        assertThatThrownBy(() -> new AppUser("  ", HASH, EMAIL, ROLE, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullRole() {
        assertThatThrownBy(() -> new AppUser(USERNAME, HASH, EMAIL, null, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateRole_changesRole() {
        AppUser user = new AppUser(USERNAME, HASH, EMAIL, UserRole.PENDING, true);
        user.updateRole(UserRole.MANAGER);
        assertThat(user.getRole()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void reconstitute_rebuildsUser() {
        AppUser user = new AppUser(USERNAME, HASH, EMAIL, ROLE, true);
        AppUser rebuilt = AppUser.reconstitute(
                user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getEmail(), user.getRole(), user.getAudit(),
                null, null, null, null, null);

        assertThat(rebuilt.getId()).isEqualTo(user.getId());
        assertThat(rebuilt.getUsername()).isEqualTo(USERNAME);
        assertThat(rebuilt.getRole()).isEqualTo(ROLE);
    }
}
