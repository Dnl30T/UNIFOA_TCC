package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.model.FormResponse
import java.time.Instant
import java.util.UUID

data class FormResponseResponseDto(
    val id: UUID,
    val formId: UUID,
    val employeeId: UUID,
    val answers: Map<UUID, Int>,
    val textAnswers: Map<UUID, String>,
    val submittedAt: Instant,
) {
    companion object {
        fun fromDomain(fr: FormResponse) = FormResponseResponseDto(
            id = fr.id,
            formId = fr.formId,
            employeeId = fr.employeeId,
            answers = fr.answers,
            textAnswers = fr.textAnswers,
            submittedAt = fr.submittedAt,
        )
    }
}
