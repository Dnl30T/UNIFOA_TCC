package com.psytrack.unformulieren.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Explicitly scopes Spring Data JPA repository scanning to the packages that
 * contain {@code JpaRepository} interfaces.
 *
 * <p>This is required because both Spring Data JPA and Spring Data Cassandra
 * are on the classpath. Without explicit scoping the auto-detection would try
 * to bootstrap every repository interface for both stores.</p>
 */
@Configuration
@EnableJpaRepositories(basePackages = {
        "com.psytrack.unformulieren.adapter.out.persistence.employee.repository",
        "com.psytrack.unformulieren.adapter.out.persistence.employeeResult.repository",
        "com.psytrack.unformulieren.adapter.out.persistence.dashboardSnapshot.repository",
        "com.psytrack.unformulieren.adapter.out.persistence.team.repository",
        "com.psytrack.unformulieren.adapter.out.persistence.teamResult.repository",
        "com.psytrack.unformulieren.adapter.out.persistence.user.repository"
})
public class JpaConfig {
}
