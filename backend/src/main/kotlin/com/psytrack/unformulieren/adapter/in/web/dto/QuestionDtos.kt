package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.enums.QuestionType
import com.psytrack.unformulieren.domain.model.Question
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class QuestionRequestDto(
    @field:NotNull val formId: UUID,
    @field:NotBlank val text: String,
    @field:NotNull val type: QuestionType,
    val required: Boolean = false,
    val config: Map<String, String> = emptyMap(),
    val order: Int = 0,
)

data class QuestionResponseDto(
    val id: UUID,
    val formId: UUID,
    val text: String,
    val type: QuestionType,
    val required: Boolean,
    val config: Map<String, String>,
    val order: Int,
) {
    companion object {
        fun fromDomain(q: Question) = QuestionResponseDto(
            id = q.id,
            formId = q.formId,
            text = q.text,
            type = q.type,
            required = q.isRequired,
            config = q.config,
            order = q.order,
        )
    }
}
