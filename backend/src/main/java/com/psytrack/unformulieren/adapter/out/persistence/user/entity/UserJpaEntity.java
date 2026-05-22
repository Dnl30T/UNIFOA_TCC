package com.psytrack.unformulieren.adapter.out.persistence.user.entity;

import java.time.Instant;
import java.util.UUID;

import com.psytrack.unformulieren.domain.enums.UserRole;
import com.psytrack.unformulieren.domain.model.UserAudit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String name;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(length = 255)
    private String company;

    @Column(name = "job_title", length = 255)
    private String jobTitle;

    @Column(name = "profile_picture_url", length = 512)
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserRole role;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "consent_date")
    private Instant consentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(nullable = false)
    private boolean anonymized;

    @Column(name = "fully_anonymized", nullable = false)
    private boolean fullyAnonymized;

    private UserJpaEntity(UUID id, String username, String passwordHash,
                          String email, UserRole role, UserAudit audit,
                          String name, String phoneNumber, String company,
                          String jobTitle, String profilePictureUrl,
                          boolean fullyAnonymized) {
        this.id                = id;
        this.username          = username;
        this.passwordHash      = passwordHash;
        this.email             = email;
        this.role              = role;
        this.consentGiven      = audit.isConsentGiven();
        this.consentDate       = audit.getConsentDate();
        this.createdAt         = audit.getCreatedAt();
        this.updatedAt         = audit.getUpdatedAt();
        this.deletedAt         = audit.getDeletedAt();
        this.anonymized        = audit.isAnonymized();
        this.name              = name;
        this.phoneNumber       = phoneNumber;
        this.company           = company;
        this.jobTitle          = jobTitle;
        this.profilePictureUrl = profilePictureUrl;
        this.fullyAnonymized   = fullyAnonymized;
    }

    public static UserJpaEntity of(UUID id, String username, String passwordHash,
                                   String email, UserRole role, UserAudit audit,
                                   String name, String phoneNumber, String company,
                                   String jobTitle, String profilePictureUrl,
                                   boolean fullyAnonymized) {
        return new UserJpaEntity(id, username, passwordHash, email, role, audit,
                                 name, phoneNumber, company, jobTitle, profilePictureUrl,
                                 fullyAnonymized);
    }
}
