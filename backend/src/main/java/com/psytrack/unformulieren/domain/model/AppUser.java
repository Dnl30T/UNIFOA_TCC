package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.UserRole;
import com.psytrack.unformulieren.domain.validation.DomainValidations;
import com.psytrack.unformulieren.domain.valueobject.Email;

/**
 * Aggregate root representing an application user.
 *
 * LGPD compliance:
 *  - Passwords are never stored in plain text ({@code passwordHash} only).
 *  - PII fields (username, email) can be fully anonymized on demand.
 *  - Explicit consent tracking is delegated to {@link UserAudit}.
 *  - Soft-delete preserves the audit trail while hiding data from active queries.
 */
public class AppUser {

    private static final String ANONYMIZED_MARKER = "anonymized_%s";
    private static final String ROLE_REQUIRED      = "role must not be null";

    private final UUID id;
    private String username;
    private String passwordHash;
    private String email;
    private String name;
    private String phoneNumber;
    private String company;
    private String jobTitle;
    private String profilePictureUrl;
    private UserRole role;
    private final UserAudit audit;
    private boolean fullyAnonymized;

    /**
     * Creates a new user. Consent must be explicitly given at registration (LGPD Art. 7).
     *
     * @param passwordHash BCrypt (or equivalent) hash — never a plain-text password.
     * @param consentGiven must be {@code true}; registration is rejected otherwise.
     */
    public AppUser(String username, String passwordHash, Email email, UserRole role, boolean consentGiven) {
        if (!consentGiven) {
            throw new IllegalArgumentException(
                    "User consent is required to create an account (LGPD Art. 7)");
        }
        this.id           = UUID.randomUUID();
        this.username     = DomainValidations.requireNonBlank(username, "username");
        this.passwordHash = DomainValidations.requireNonBlank(passwordHash, "passwordHash");
        this.email        = Objects.requireNonNull(email, "email must not be null").getValue();
        this.role         = Objects.requireNonNull(role, ROLE_REQUIRED);
        this.audit        = UserAudit.forNewUser();
    }

    private AppUser(UUID id, String username, String passwordHash,
                    String email, UserRole role, UserAudit audit,
                    String name, String phoneNumber, String company,
                    String jobTitle, String profilePictureUrl,
                    boolean fullyAnonymized) {
        this.id                = Objects.requireNonNull(id, "id must not be null");
        this.username          = username;
        this.passwordHash      = passwordHash;
        this.email             = email;
        this.role              = Objects.requireNonNull(role, ROLE_REQUIRED);
        this.audit             = Objects.requireNonNull(audit, "audit must not be null");
        this.name              = name;
        this.phoneNumber       = phoneNumber;
        this.company           = company;
        this.jobTitle          = jobTitle;
        this.profilePictureUrl = profilePictureUrl;
        this.fullyAnonymized   = fullyAnonymized;
    }

    /**
     * Rebuilds a domain object from a persistence record.
     * Email is accepted as a raw {@code String} to support anonymized records
     * whose value would not pass {@link Email} validation.
     */
    public static AppUser reconstitute(UUID id, String username, String passwordHash,
                                       String email, UserRole role, UserAudit audit,
                                       String name, String phoneNumber, String company,
                                       String jobTitle, String profilePictureUrl,
                                       boolean fullyAnonymized) {
        return new AppUser(id, username, passwordHash, email, role, audit,
                           name, phoneNumber, company, jobTitle, profilePictureUrl,
                           fullyAnonymized);
    }

    public void updateEmail(Email newEmail) {
        this.email = Objects.requireNonNull(newEmail, "email must not be null").getValue();
        audit.touch();
    }

    public void updatePasswordHash(String newPasswordHash) {
        this.passwordHash = DomainValidations.requireNonBlank(newPasswordHash, "passwordHash");
        audit.touch();
    }

    public void updateRole(UserRole newRole) {
        this.role = Objects.requireNonNull(newRole, ROLE_REQUIRED);
        audit.touch();
    }

    public void recordConsent() {
        audit.recordConsent();
    }

    /**
     * Revokes consent and immediately anonymizes the account (LGPD Art. 15 / Art. 18).
     */
    public void revokeConsent() {
        audit.revokeConsent();
        anonymize();
    }

    /**
     * Replaces all PII fields with non-identifiable markers and soft-deletes the account.
     * Once anonymized, the record cannot be de-anonymized.
     */
    public void anonymize() {
        String marker     = String.format(ANONYMIZED_MARKER, id);
        this.username     = marker;
        this.email        = marker;
        this.passwordHash = marker;
        audit.anonymize();
    }

    public void updateProfile(String name, String phoneNumber, String company, String jobTitle) {
        this.name        = name;
        this.phoneNumber = phoneNumber;
        this.company     = company;
        this.jobTitle    = jobTitle;
        audit.touch();
    }

    public void updateProfilePicture(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
        audit.touch();
    }

    public void softDelete() {
        audit.softDelete();
    }


    public boolean isDeleted()         { return audit.getDeletedAt() != null; }
    public boolean isAnonymized()      { return audit.isAnonymized(); }
    public boolean isFullyAnonymized() { return fullyAnonymized; }

    public void setFullyAnonymized(boolean fullyAnonymized) {
        this.fullyAnonymized = fullyAnonymized;
        audit.touch();
    }

    public UUID      getId()                { return id; }
    public String    getUsername()          { return username; }
    public String    getPasswordHash()      { return passwordHash; }
    public String    getEmail()             { return email; }
    public String    getName()              { return name; }
    public String    getPhoneNumber()       { return phoneNumber; }
    public String    getCompany()           { return company; }
    public String    getJobTitle()          { return jobTitle; }
    public String    getProfilePictureUrl() { return profilePictureUrl; }
    public UserRole  getRole()              { return role; }
    public UserAudit getAudit()             { return audit; }

    public boolean isConsentGiven() { return audit.isConsentGiven(); }
    public Instant getConsentDate() { return audit.getConsentDate(); }
    public Instant getCreatedAt()   { return audit.getCreatedAt(); }
    public Instant getUpdatedAt()   { return audit.getUpdatedAt(); }
    public Instant getDeletedAt()   { return audit.getDeletedAt(); }
}
