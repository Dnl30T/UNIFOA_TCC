package com.psytrack.unformulieren.adapter.out.persistence.report.entity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntity {

    @PrimaryKey
    private ReportPrimaryKey key;

    @Column("form_id")
    private UUID formId;

    @Column("name")
    private String name;

    @Column("respondent_ids")
    private List<UUID> respondentIds;

    @Column("respondent_count")
    private int respondentCount;

    @Column("average_score")
    private double averageScore;

    @Column("general_risk")
    private String generalRisk;

    @Column("risk_distribution")
    private Map<String, Integer> riskDistribution;

    @Column("generated_at")
    private Instant generatedAt;

    @Column("created_by")
    private String createdBy;
}
