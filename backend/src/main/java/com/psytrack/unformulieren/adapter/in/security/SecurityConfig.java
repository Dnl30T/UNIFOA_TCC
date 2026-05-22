package com.psytrack.unformulieren.adapter.in.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── CORS preflight ─────────────────────────────────────────────────
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers("/auth/login", "/auth/register/**").permitAll()
                .requestMatchers("/auth/claim-role").hasRole("PENDING")
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // ── Teams: Manager creates their own team ──────────────────────────
                .requestMatchers(HttpMethod.POST, "/teams", "/teams/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/teams/join").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.PUT, "/teams", "/teams/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/teams", "/teams/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/teams", "/teams/**").authenticated()

                // ── Forms & Questions: Counselor is the clinical process owner ──────
                .requestMatchers(HttpMethod.POST, "/forms", "/forms/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.PUT, "/forms", "/forms/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.DELETE, "/forms", "/forms/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.GET, "/forms", "/forms/**").authenticated()

                .requestMatchers(HttpMethod.POST, "/questions", "/questions/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.PUT, "/questions", "/questions/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.DELETE, "/questions", "/questions/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.GET, "/questions", "/questions/**").authenticated()

                // ── Form Responses: Employees submit; Counselor/Admin read ──────────
                .requestMatchers(HttpMethod.POST, "/form-responses", "/form-responses/**").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/form-responses", "/form-responses/**").hasAnyRole("COUNSELOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/form-responses", "/form-responses/**").hasAnyRole("COUNSELOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/form-responses", "/form-responses/**").hasAnyRole("COUNSELOR", "ADMIN")

                // ── Results: Counselor submits; visibility scoped at service layer ──
                .requestMatchers(HttpMethod.POST, "/employee-results", "/employee-results/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.PUT, "/employee-results", "/employee-results/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.DELETE, "/employee-results", "/employee-results/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.GET, "/employee-results", "/employee-results/**").hasAnyRole("EMPLOYEE", "COUNSELOR", "MANAGER", "ADMIN")

                .requestMatchers(HttpMethod.POST, "/team-results", "/team-results/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.PUT, "/team-results", "/team-results/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.DELETE, "/team-results", "/team-results/**").hasRole("COUNSELOR")
                .requestMatchers(HttpMethod.GET, "/team-results", "/team-results/**").hasAnyRole("MANAGER", "COUNSELOR", "ADMIN")

                // ── Form Submissions: Employee posts and reads own; Counselor/Manager/Admin read all ──
                .requestMatchers(HttpMethod.POST, "/form-submissions", "/form-submissions/**").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/form-submissions", "/form-submissions/**").authenticated()

                // ── Therapist Evaluations: Counselor/Admin write; authenticated read ──
                .requestMatchers(HttpMethod.POST, "/therapist-evaluations", "/therapist-evaluations/**").hasAnyRole("COUNSELOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/therapist-evaluations", "/therapist-evaluations/**").hasAnyRole("COUNSELOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/therapist-evaluations", "/therapist-evaluations/**").authenticated()

                // ── Reports: Counselor/Admin generate; Manager/Counselor/Admin read ──
                .requestMatchers(HttpMethod.POST, "/reports", "/reports/**").hasAnyRole("COUNSELOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/reports", "/reports/**").hasAnyRole("COUNSELOR", "MANAGER", "ADMIN")

                // ── Employees: reads authenticated; writes Admin only ────────────────
                .requestMatchers(HttpMethod.GET, "/employees", "/employees/**").authenticated()

                // ── User profile (self-service, optional) ─────────────────────────
                .requestMatchers("/users/me/**").authenticated()

                .anyRequest().hasRole("ADMIN")
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
