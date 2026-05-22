package com.psytrack.unformulieren.adapter.in.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.UserRepositoryPort;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepositoryPort userRepositoryPort;

    public AppUserDetailsService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var appUser = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new User(
                appUser.getUsername(),
                appUser.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()))
        );
    }
}
