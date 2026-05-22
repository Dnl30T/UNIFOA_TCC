package com.psytrack.unformulieren.adapter.out.persistence.report.entity;

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

@PrimaryKeyClass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReportPrimaryKey implements Serializable {

    @PrimaryKeyColumn(name = "team_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private UUID teamId;

    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private UUID id;
}
