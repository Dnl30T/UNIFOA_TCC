package com.psytrack.unformulieren.adapter.out.persistence.form.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ScyllaDB/Cassandra entity for the {@code forms} table.
 * <p>
 * The {@code questions} column stores a denormalised LIST of frozen
 * {@link QuestionStructureUdt} UDTs so that the Form Renderer can retrieve
 * the entire form — metadata and all questions — with a single partition read.
 * </p>
 * Partition key: {@code form_id} — one partition per form.
 */
@Table("forms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormCassandraEntity {

    @PrimaryKey("form_id")
    private UUID formId;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("status")
    private String status;

    @Column("created_by")
    private String createdBy;

    @Column("team_ids")
    @CassandraType(type = CassandraType.Name.LIST, typeArguments = CassandraType.Name.UUID)
    private List<UUID> teamIds = new ArrayList<>();

    @Column("questions")
    @CassandraType(type = CassandraType.Name.LIST, typeArguments = CassandraType.Name.UDT,
            userTypeName = "question_structure")
    private List<QuestionStructureUdt> questions = new ArrayList<>();

    public static FormCassandraEntity of(UUID formId, String title, String description,
                                         String status, String createdBy, List<UUID> teamIds,
                                         List<QuestionStructureUdt> questions) {
        return new FormCassandraEntity(formId, title, description, status, createdBy,
                teamIds == null ? new ArrayList<>() : new ArrayList<>(teamIds),
                questions == null ? new ArrayList<>() : new ArrayList<>(questions));
    }
}
