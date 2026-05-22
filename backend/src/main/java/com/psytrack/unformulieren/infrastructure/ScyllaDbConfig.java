package com.psytrack.unformulieren.infrastructure;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

/**
 * Cassandra/ScyllaDB configuration.
 *
 * <p>Extends {@link AbstractCassandraConfiguration} so Spring Data Cassandra
 * manages the {@code CqlSession} bean. The keyspace is created automatically
 * via {@link #getKeyspaceCreations()} before any table DDL runs.</p>
 *
 * <p>JPA repositories are configured in {@link JpaConfig}.</p>
 */
@Configuration
@EnableCassandraRepositories(basePackages = {
        "com.psytrack.unformulieren.adapter.out.persistence.form.repository",
        "com.psytrack.unformulieren.adapter.out.persistence.formResponse.repository",
    "com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.repository",
    "com.psytrack.unformulieren.adapter.out.persistence.report.repository"
})
public class ScyllaDbConfig extends AbstractCassandraConfiguration {

    @Value("${spring.cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${spring.cassandra.port:9042}")
    private int port;

    @Value("${spring.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Value("${spring.cassandra.keyspace-name:psytrack}")
    private String keyspaceName;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        return List.of(
                CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                        .ifNotExists()
                        .with(KeyspaceOption.DURABLE_WRITES, true)
                        .withSimpleReplication(1));
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[]{
                "com.psytrack.unformulieren.adapter.out.persistence.form.entity",
                "com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity",
                "com.psytrack.unformulieren.adapter.out.persistence.report.entity"
        };
    }
}
