package com.psytrack.unformulieren.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object that groups all LGPD-related audit metadata for {@link AppUser}.
 *
 * Extracting these fields prevents the {@code AppUser} constructor from
 * exceeding the recommended parameter limit and makes the consent/audit
 * lifecycle explicit.
 */
public final class UserAudit {

    private boolean consentGiven;
    private Instant consentDate;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private boolean anonymized;

    private UserAudit(boolean consentGiven, Instant consentDate,
                      Instant createdAt, Instant updatedAt,
                      Instant deletedAt, boolean anonymized) {
        this.consentGiven = consentGiven;
        this.consentDate = consentDate;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.deletedAt = deletedAt;
        this.anonymized = anonymized;
    }

    /** Creates audit metadata for a brand-new user (consent already given). */
    public static UserAudit forNewUser() {
        Instant now = Instant.now();
        return new UserAudit(true, now, now, now, null, false);
    }

    /** Rebuilds audit metadata from a persistence record. */
    public static UserAudit reconstitute(boolean consentGiven, Instant consentDate,
                                         Instant createdAt, Instant updatedAt,
                                         Instant deletedAt, boolean anonymized) {
        return new UserAudit(consentGiven, consentDate, createdAt, updatedAt, deletedAt, anonymized);
    }

    // -------------------------------------------------------------------------
    // Lifecycle mutations
    // -------------------------------------------------------------------------

    void recordConsent() {
        this.consentGiven = true;
        this.consentDate = Instant.now();
        this.updatedAt = Instant.now();
    }

    void revokeConsent() {
        this.consentGiven = false;
        this.updatedAt = Instant.now();
    }

    /** Replaces all mutable fields with an anonymized marker. */
    void anonymize() {
        this.anonymized = true;
        this.updatedAt = Instant.now();
        if (this.deletedAt == null) {
            this.deletedAt = Instant.now();
        }
    }

    void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    void touch() {
        this.updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public boolean isConsentGiven() { return consentGiven; }
    public Instant getConsentDate()  { return consentDate; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }
    public Instant getDeletedAt()    { return deletedAt; }
    public boolean isAnonymized()    { return anonymized; }
}
