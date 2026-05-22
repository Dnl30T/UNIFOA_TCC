package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.enums.Status
import com.psytrack.unformulieren.domain.model.Form
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class FormRequestDto(
    @field:NotBlank val title: String,
    val description: String = "",
    @field:NotNull val status: Status,
    val teamIds: List<UUID>? = null,
)

data class FormResponseDto(
    val id: UUID,
    val title: String,
    val description: String,
    val status: Status,
    val teamIds: List<UUID>,
    val questions: List<QuestionResponseDto>,
) {
    companion object {
        fun fromDomain(form: Form) = FormResponseDto(
            id = form.id,
            title = form.title,
            description = form.description,
            status = form.status,
            teamIds = form.teamIds,
            questions = form.questions.map(QuestionResponseDto::fromDomain),
        )
    }
}
