package com.psytrack.unformulieren.adapter.out.persistence.user.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.psytrack.unformulieren.adapter.out.persistence.user.mapper.UserPersistenceMapper;
import com.psytrack.unformulieren.adapter.out.persistence.user.repository.UserJpaRepository;
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort;
import com.psytrack.unformulieren.domain.model.AppUser;

@Repository
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
        this.mapper = new UserPersistenceMapper();
    }

    @Override
    public AppUser save(AppUser user) {
        return mapper.toDomain(repository.save(mapper.toJpa(user)));
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }
}
