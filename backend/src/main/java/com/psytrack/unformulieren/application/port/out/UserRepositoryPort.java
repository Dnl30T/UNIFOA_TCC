package com.psytrack.unformulieren.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.model.AppUser;

public interface UserRepositoryPort {

    AppUser save(AppUser user);

    Optional<AppUser> findById(UUID id);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);
}
