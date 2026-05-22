package com.psytrack.unformulieren.adapter.out.persistence.formResponse.entity;

import java.io.Serializable;
import java.util.UUID;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite partition key for the {@code form_responses} table.
 * One row per {@code (form_id, employee_id)} pair — a single row holds
 * the entire submission including all answers.
 */
@PrimaryKeyClass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FormResponsePrimaryKey implements Serializable {

    @PrimaryKeyColumn(name = "form_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private UUID formId;

    @PrimaryKeyColumn(name = "employee_id", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private UUID employeeId;
}
