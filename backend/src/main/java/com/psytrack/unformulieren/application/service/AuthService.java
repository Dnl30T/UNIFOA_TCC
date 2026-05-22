package com.psytrack.unformulieren.application.service;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.adapter.out.security.JwtTokenService;
import com.psytrack.unformulieren.application.port.out.TeamRepositoryPort;
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort;
import com.psytrack.unformulieren.domain.exception.TeamNotFoundException;
import com.psytrack.unformulieren.domain.enums.UserRole;
import com.psytrack.unformulieren.domain.model.AppUser;
import com.psytrack.unformulieren.domain.model.Team;
import com.psytrack.unformulieren.domain.valueobject.Email;

@Service
public class AuthService {

    private final UserRepositoryPort userRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepositoryPort userRepositoryPort,
                       TeamRepositoryPort teamRepositoryPort,
                       EmployeeService employeeService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       AuthenticationManager authenticationManager) {
        this.userRepositoryPort = userRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
    }

    public String login(String email, String password) {
        AppUser user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), password));
        return jwtTokenService.generateToken(user.getUsername(), user.getRole().name());
    }

    public String registerEmployee(String username, String password, String email, String teamCode,
                                   String name, String phoneNumber, String company, String jobTitle) {
        String normalizedTeamCode = normalizeTeamCode(teamCode);
        Team team = teamRepositoryPort.findByTeamCode(normalizedTeamCode)
                .orElseThrow(() -> new TeamNotFoundException(normalizedTeamCode));
        String token = createUser(username, password, email, UserRole.EMPLOYEE, name, phoneNumber, company, jobTitle);
        // Create the employee record linked to this user and team
        AppUser savedUser = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found after creation: " + username));
        String employeeName = (name != null && !name.isBlank()) ? name : username;
        employeeService.hireEmployee(employeeName, savedUser.getId(), team.getId());
        return token;
    }

    public String registerStaff(String username, String password, String email,
                                String name, String phoneNumber, String company, String jobTitle) {
        return createUser(username, password, email, UserRole.PENDING, name, phoneNumber, company, jobTitle);
    }

    public String claimRole(String username, UserRole requestedRole) {
        if (requestedRole != UserRole.COUNSELOR && requestedRole != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only COUNSELOR or MANAGER roles can be claimed");
        }
        AppUser user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        if (user.getRole() != UserRole.PENDING) {
            throw new IllegalStateException("Only PENDING users can claim a role");
        }
        user.updateRole(requestedRole);
        userRepositoryPort.save(user);
        return jwtTokenService.generateToken(username, requestedRole.name());
    }

    private String createUser(String username, String password, String email, UserRole role,
                              String name, String phoneNumber, String company, String jobTitle) {
        userRepositoryPort.findByUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Username already taken: " + username);
        });
        userRepositoryPort.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("Email already in use: " + email);
        });
        AppUser user = new AppUser(username, passwordEncoder.encode(password), new Email(email), role, true);
        if (name != null || phoneNumber != null || company != null || jobTitle != null) {
            user.updateProfile(name, phoneNumber, company, jobTitle);
        }
        userRepositoryPort.save(user);
        return jwtTokenService.generateToken(username, role.name());
    }

    private String normalizeTeamCode(String teamCode) {
        if (teamCode == null || teamCode.isBlank()) {
            throw new IllegalArgumentException("teamCode must not be blank");
        }
        return teamCode.trim().toUpperCase(Locale.ROOT);
    }
}
