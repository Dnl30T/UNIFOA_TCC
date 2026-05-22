package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.enums.FormResponseStatus
import com.psytrack.unformulieren.domain.enums.RiskLevel
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class FormSubmissionRequestDto(
    @field:NotNull val formId: UUID,
    @field:NotNull val employeeId: UUID,
    val answers: List<AnswerDto> = emptyList(),
) {
    data class AnswerDto(
        val questionId: UUID,
        val value: Int = 0,
        /** Non-null for TEXT / LONG_TEXT / DATE questions */
        val textValue: String? = null,
    )
}

data class FormSubmissionResponseDto(
    val formId: UUID,
    val employeeId: UUID,
    val answerCount: Int,
    val submittedAt: Instant,
    val responseStatus: FormResponseStatus,
    val helperScore: Int? = null,
    val finalScore: Int? = null,
    val burnoutRiskPreAnalysis: RiskLevel? = null,
)
