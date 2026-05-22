package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.enums.RiskLevel
import com.psytrack.unformulieren.domain.model.EmployeeResult
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class EmployeeResultRequestDto(
    @field:NotNull val employeeId: UUID,
    @field:NotNull val formId: UUID,
    @field:Min(0) val score: Int,
    @field:NotNull val riskLevel: RiskLevel,
    val calculatedAt: Instant = Instant.now(),
)

data class EmployeeResultResponseDto(
    val id: UUID,
    val employeeId: UUID,
    val formId: UUID,
    val score: Int,
    val helperScore: Int,
    val finalScore: Int?,
    val riskLevel: RiskLevel,
    val calculatedAt: Instant,
) {
    companion object {
        fun fromDomain(er: EmployeeResult) = EmployeeResultResponseDto(
            id = er.id,
            employeeId = er.employeeId,
            formId = er.formId,
            score = er.displayScore,
            helperScore = er.helperScore,
            finalScore = er.finalScore,
            riskLevel = er.riskLevel,
            calculatedAt = er.calculatedAt,
        )
    }
}

data class EmployeeResultFinalizeRequestDto(
    @field:NotNull val employeeId: UUID,
    @field:NotNull val formId: UUID,
    @field:Min(0) val finalScore: Int,
)
