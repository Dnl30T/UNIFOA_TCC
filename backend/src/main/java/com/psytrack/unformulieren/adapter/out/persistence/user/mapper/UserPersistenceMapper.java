package com.psytrack.unformulieren.adapter.out.persistence.user.mapper;

import java.util.Objects;

import com.psytrack.unformulieren.adapter.out.persistence.user.entity.UserJpaEntity;
import com.psytrack.unformulieren.domain.model.AppUser;
import com.psytrack.unformulieren.domain.model.UserAudit;

public class UserPersistenceMapper {

    public UserJpaEntity toJpa(AppUser domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        return UserJpaEntity.of(
                domain.getId(),
                domain.getUsername(),
                domain.getPasswordHash(),
                domain.getEmail(),
                domain.getRole(),
                domain.getAudit(),
                domain.getName(),
                domain.getPhoneNumber(),
                domain.getCompany(),
                domain.getJobTitle(),
                domain.getProfilePictureUrl(),
                domain.isFullyAnonymized());
    }

    public AppUser toDomain(UserJpaEntity jpa) {
        Objects.requireNonNull(jpa, "jpa must not be null");
        UserAudit audit = UserAudit.reconstitute(
                jpa.isConsentGiven(),
                jpa.getConsentDate(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt(),
                jpa.getDeletedAt(),
                jpa.isAnonymized());
        return AppUser.reconstitute(
                jpa.getId(),
                jpa.getUsername(),
                jpa.getPasswordHash(),
                jpa.getEmail(),
                jpa.getRole(),
                audit,
                jpa.getName(),
                jpa.getPhoneNumber(),
                jpa.getCompany(),
                jpa.getJobTitle(),
                jpa.getProfilePictureUrl(),
                jpa.isFullyAnonymized());
    }
}
