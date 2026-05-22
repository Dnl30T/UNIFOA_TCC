package com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.entity;

import java.time.Instant;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("therapist_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TherapistEvaluationEntity {

    @PrimaryKey
    private TherapistEvaluationPrimaryKey key;

    @Column("occupational_context")
    private String occupationalContext;

    @Column("team_dynamics")
    private String teamDynamics;

    @Column("psychosomatic_symptoms")
    private String psychosomaticSymptoms;

    @Column("cognitive_emotional_changes")
    private String cognitiveEmotionalChanges;

    @Column("exhaustion_score")
    private Integer exhaustionScore;

    @Column("exhaustion_justification")
    private String exhaustionJustification;

    @Column("depersonalization_score")
    private Integer depersonalizationScore;

    @Column("depersonalization_justification")
    private String depersonalizationJustification;

    @Column("differential_diagnosis")
    private String differentialDiagnosis;

    @Column("intervention_plan")
    private String interventionPlan;

    @Column("internal_note")
    private String internalNote;

    @Column("hr_summary")
    private String hrSummary;

    @Column("closing_commentary")
    private String closingCommentary;

    @Column("stress_score")
    private Integer stressScore;

    @Column("sleep_score")
    private Integer sleepScore;

    @Column("overload_score")
    private Integer overloadScore;

    @Column("fatigue_score")
    private Integer fatigueScore;

    @Column("disengagement_score")
    private Integer disengagementScore;

    @Column("isolation_score")
    private Integer isolationScore;

    @Column("status")
    private String status;

    @Column("published_at")
    private Instant publishedAt;

    @Column("created_by")
    private String createdBy;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
