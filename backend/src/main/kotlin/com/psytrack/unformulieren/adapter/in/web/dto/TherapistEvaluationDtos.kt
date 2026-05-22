package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.enums.FormResponseStatus
import com.psytrack.unformulieren.domain.enums.RiskLevel
import com.psytrack.unformulieren.domain.enums.TherapistEvaluationStatus
import java.time.Instant
import java.util.UUID

data class TherapistEvaluationRequestDto(
    val occupationalContext: String? = null,
    val teamDynamics: String? = null,
    val psychosomaticSymptoms: String? = null,
    val cognitiveEmotionalChanges: String? = null,
    val exhaustionScore: Int? = null,
    val exhaustionJustification: String? = null,
    val depersonalizationScore: Int? = null,
    val depersonalizationJustification: String? = null,
    val differentialDiagnosis: String? = null,
    val interventionPlan: String? = null,
    val internalNote: String? = null,
    val hrSummary: String? = null,
    val closingCommentary: String? = null,
    val stressScore: Int? = null,
    val sleepScore: Int? = null,
    val overloadScore: Int? = null,
    val fatigueScore: Int? = null,
    val disengagementScore: Int? = null,
    val isolationScore: Int? = null,
    val status: TherapistEvaluationStatus? = null,
)

data class TherapistEvaluationResponseDto(
    val formId: UUID,
    val employeeId: UUID,
    val occupationalContext: String?,
    val teamDynamics: String?,
    val psychosomaticSymptoms: String?,
    val cognitiveEmotionalChanges: String?,
    val exhaustionScore: Int?,
    val exhaustionJustification: String?,
    val depersonalizationScore: Int?,
    val depersonalizationJustification: String?,
    val differentialDiagnosis: String?,
    val interventionPlan: String?,
    /** Only included when requester is COUNSELOR/ADMIN */
    val internalNote: String?,
    val hrSummary: String?,
    val closingCommentary: String?,
    val stressScore: Int?,
    val sleepScore: Int?,
    val overloadScore: Int?,
    val fatigueScore: Int?,
    val disengagementScore: Int?,
    val isolationScore: Int?,
    val status: TherapistEvaluationStatus,
    val publishedAt: Instant?,
    val createdBy: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

data class TherapistComprehensiveResponseDto(
    val formId: UUID,
    val employeeId: UUID,
    val answeredQuestions: Int,
    val textQuestions: Int,
    val responseStatus: FormResponseStatus,
    val helperScore: Int?,
    val finalScore: Int?,
    val burnoutRiskPreAnalysis: RiskLevel?,
    val submittedAt: Instant,
    val closedAt: Instant?,
    val therapistEvaluation: TherapistEvaluationResponseDto?,
)

data class PublishBatchRequestDto(
    val employeeIds: List<UUID> = emptyList(),
)
