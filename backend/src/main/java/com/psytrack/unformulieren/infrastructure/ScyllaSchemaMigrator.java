package com.psytrack.unformulieren.infrastructure;

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Ensures new columns exist in ScyllaDB tables on every startup.
 * Runs as an {@link InitializingBean} so it executes before any Cassandra
 * repository tries to INSERT/SELECT those columns.
 *
 * <p>Uses {@code system_schema.columns} to test column existence instead of
 * {@code ALTER TABLE ... ADD IF NOT EXISTS}, which is not supported by all
 * ScyllaDB versions.</p>
 */
@Component
@DependsOn("cassandraSession")
public class ScyllaSchemaMigrator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ScyllaSchemaMigrator.class);

    private final CqlSession cqlSession;
    private final String keyspace;

    public ScyllaSchemaMigrator(CqlSession cqlSession,
                                @Value("${spring.cassandra.keyspace-name:psytrack}") String keyspace) {
        this.cqlSession = cqlSession;
        this.keyspace = keyspace;
    }

    @Override
    public void afterPropertiesSet() {
        addColumnIfMissing("forms", "created_by", "text");
        addColumnIfMissing("forms", "team_ids",   "list<uuid>");
        addColumnIfMissing("form_responses", "response_status", "text");
        addColumnIfMissing("form_responses", "closed_at", "timestamp");
        addColumnIfMissing("therapist_evaluations", "closing_commentary", "text");
        addColumnIfMissing("therapist_evaluations", "stress_score", "int");
        addColumnIfMissing("therapist_evaluations", "sleep_score", "int");
        addColumnIfMissing("therapist_evaluations", "overload_score", "int");
        addColumnIfMissing("therapist_evaluations", "fatigue_score", "int");
        addColumnIfMissing("therapist_evaluations", "disengagement_score", "int");
        addColumnIfMissing("therapist_evaluations", "isolation_score", "int");
        addColumnIfMissing("therapist_evaluations", "status", "text");
        addColumnIfMissing("therapist_evaluations", "published_at", "timestamp");
    }

    private void addColumnIfMissing(String table, String column, String cqlType) {
        try {
            var rs = cqlSession.execute(
                    "SELECT column_name FROM system_schema.columns" +
                    " WHERE keyspace_name='" + keyspace + "'" +
                    " AND table_name='" + table + "'" +
                    " AND column_name='" + column + "'"
            );
            if (rs.one() == null) {
                cqlSession.execute(
                        "ALTER TABLE " + keyspace + "." + table +
                        " ADD " + column + " " + cqlType
                );
                log.info("ScyllaDB migration: added column {}.{} ({})", table, column, cqlType);
            } else {
                log.debug("ScyllaDB migration: {}.{} already exists — skipping", table, column);
            }
        } catch (Exception e) {
            log.error("ScyllaDB migration FAILED for {}.{}: {}", table, column, e.getMessage(), e);
            throw new IllegalStateException(
                    "ScyllaDB migration failed — cannot start without column " + table + "." + column, e);
        }
    }
}
