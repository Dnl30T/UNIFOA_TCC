package com.psytrack.unformulieren.application.service;

import com.psytrack.unformulieren.adapter.out.security.JwtTokenService;
import com.psytrack.unformulieren.application.port.out.TeamRepositoryPort;
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort;
import com.psytrack.unformulieren.domain.enums.UserRole;
import com.psytrack.unformulieren.domain.exception.TeamNotFoundException;
import com.psytrack.unformulieren.domain.model.AppUser;
import com.psytrack.unformulieren.domain.model.Team;
import com.psytrack.unformulieren.domain.valueobject.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private TeamRepositoryPort teamRepositoryPort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private AppUser existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new AppUser("alice", "$2a$10$hash", new Email("alice@example.com"), UserRole.EMPLOYEE, true);
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_returnsToken_whenCredentialsValid() {
        given(userRepositoryPort.findByEmail("alice@example.com")).willReturn(Optional.of(existingUser));
        given(jwtTokenService.generateToken("alice", "EMPLOYEE")).willReturn("jwt-token");

        String token = authService.login("alice@example.com", "secret");

        assertThat(token).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_throws_whenUserNotFoundAfterAuth() {
        given(userRepositoryPort.findByEmail("ghost@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@example.com", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost@example.com");
    }

    // ── registerEmployee ─────────────────────────────────────────────────────

    @Test
    void registerEmployee_returnsToken_whenTeamExistsAndUsernameAvailable() {
        Team team = new Team("Alpha", UUID.randomUUID(), "ALPHA-01");
        given(teamRepositoryPort.findByTeamCode("ALPHA-01")).willReturn(Optional.of(team));
        given(userRepositoryPort.findByUsername("bob")).willReturn(Optional.empty());
        given(userRepositoryPort.findByEmail("bob@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("pass")).willReturn("$hashed");
        given(jwtTokenService.generateToken("bob", "EMPLOYEE")).willReturn("token-for-bob");

        String token = authService.registerEmployee("bob", "pass", "bob@example.com", "alpha-01", null, null, null, null);

        assertThat(token).isEqualTo("token-for-bob");
        verify(userRepositoryPort).save(any(AppUser.class));
    }

    @Test
    void registerEmployee_normalizesTeamCodeToUppercase() {
        given(teamRepositoryPort.findByTeamCode("ALPHA-01")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerEmployee("bob", "pass", "bob@example.com", "alpha-01", null, null, null, null))
                .isInstanceOf(TeamNotFoundException.class);
        verify(teamRepositoryPort).findByTeamCode("ALPHA-01");
    }

    @Test
    void registerEmployee_throws_whenTeamNotFound() {
        given(teamRepositoryPort.findByTeamCode("NOPE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerEmployee("bob", "pass", "bob@example.com", "NOPE", null, null, null, null))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void registerEmployee_throws_whenUsernameAlreadyTaken() {
        Team team = new Team("Alpha", UUID.randomUUID(), "ALPHA-01");
        given(teamRepositoryPort.findByTeamCode("ALPHA-01")).willReturn(Optional.of(team));
        given(userRepositoryPort.findByUsername("alice")).willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.registerEmployee("alice", "pass", "alice@example.com", "ALPHA-01", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void registerEmployee_throws_whenTeamCodeBlank() {
        assertThatThrownBy(() -> authService.registerEmployee("bob", "pass", "bob@example.com", "  ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teamCode");
    }

    // ── registerStaff ────────────────────────────────────────────────────────

    @Test
    void registerStaff_returnsToken_withPendingRole() {
        given(userRepositoryPort.findByUsername("carol")).willReturn(Optional.empty());
        given(userRepositoryPort.findByEmail("carol@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("$hashed");
        given(jwtTokenService.generateToken("carol", "PENDING")).willReturn("pending-token");

        String token = authService.registerStaff("carol", "pass", "carol@example.com", null, null, null, null);

        assertThat(token).isEqualTo("pending-token");
        verify(userRepositoryPort).save(any(AppUser.class));
    }

    @Test
    void registerStaff_throws_whenUsernameAlreadyTaken() {
        given(userRepositoryPort.findByUsername("alice")).willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.registerStaff("alice", "pass", "alice2@example.com", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── claimRole ────────────────────────────────────────────────────────────

    @Test
    void claimRole_succeeds_forManager() {
        AppUser pending = new AppUser("dave", "$hash", new Email("dave@example.com"), UserRole.PENDING, true);
        given(userRepositoryPort.findByUsername("dave")).willReturn(Optional.of(pending));
        given(jwtTokenService.generateToken("dave", "MANAGER")).willReturn("manager-token");

        String token = authService.claimRole("dave", UserRole.MANAGER);

        assertThat(token).isEqualTo("manager-token");
        verify(userRepositoryPort).save(pending);
    }

    @Test
    void claimRole_succeeds_forCounselor() {
        AppUser pending = new AppUser("eve", "$hash", new Email("eve@example.com"), UserRole.PENDING, true);
        given(userRepositoryPort.findByUsername("eve")).willReturn(Optional.of(pending));
        given(jwtTokenService.generateToken("eve", "COUNSELOR")).willReturn("counselor-token");

        String token = authService.claimRole("eve", UserRole.COUNSELOR);

        assertThat(token).isEqualTo("counselor-token");
    }

    @Test
    void claimRole_throws_forEmployeeRole() {
        assertThatThrownBy(() -> authService.claimRole("alice", UserRole.EMPLOYEE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COUNSELOR or MANAGER");
    }

    @Test
    void claimRole_throws_forAdminRole() {
        assertThatThrownBy(() -> authService.claimRole("alice", UserRole.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void claimRole_throws_whenUserNotFound() {
        given(userRepositoryPort.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.claimRole("ghost", UserRole.MANAGER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void claimRole_throws_whenUserNotPending() {
        given(userRepositoryPort.findByUsername("alice")).willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.claimRole("alice", UserRole.MANAGER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }
}
