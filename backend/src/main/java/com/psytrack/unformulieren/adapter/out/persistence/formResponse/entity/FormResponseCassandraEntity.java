package com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ScyllaDB/Cassandra entity for the {@code form_responses} table.
 * One row per {@code (form_id, employee_id)} — numeric answers in
 * {@code map<uuid, int>}, free-text answers in {@code map<uuid, text>}.
 */
@Table("form_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormResponseCassandraEntity {

    @PrimaryKey
    private FormResponsePrimaryKey key;

    @Column("submission_id")
    private UUID submissionId;

    @Column("answers")
    private Map<UUID, Integer> answers;

    @Column("text_answers")
    private Map<UUID, String> textAnswers;

    @Column("submitted_at")
    private Instant submittedAt;

    @Column("response_status")
    private String responseStatus;

    @Column("closed_at")
    private Instant closedAt;
}
