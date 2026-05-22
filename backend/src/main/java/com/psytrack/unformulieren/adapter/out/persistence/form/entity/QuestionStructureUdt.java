package com.psytrack.unformulieren.adapter.out.persistence.form.entity;

import java.util.Map;
import java.util.UUID;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cassandra User-Defined Type that mirrors a single Question plus its config map.
 * Instances of this UDT are stored inline inside the {@code questions} LIST column
 * of the {@code forms} table, enabling a zero-JOIN Form Renderer read.
 */
@UserDefinedType("question_structure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStructureUdt {

    @Column("id")
    private UUID id;

    @Column("form_id")
    private UUID formId;

    @Column("text")
    private String text;

    @Column("type")
    private String type;

    @Column("required")
    private boolean required;

    @Column("config")
    @CassandraType(type = CassandraType.Name.MAP,
            typeArguments = {CassandraType.Name.TEXT, CassandraType.Name.TEXT})
    private Map<String, String> config;

    @Column("display_order")
    private int displayOrder;
}
