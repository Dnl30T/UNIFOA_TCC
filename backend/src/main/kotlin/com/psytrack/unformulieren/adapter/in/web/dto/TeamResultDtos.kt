package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.enums.RiskLevel
import com.psytrack.unformulieren.domain.model.TeamResult
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class TeamResultRequestDto(
    @field:NotNull val teamId: UUID,
    @field:NotNull val formId: UUID,
    @field:Min(0) val averageScore: Double,
    val riskLevelDistribution: Map<RiskLevel, Int> = emptyMap(),
    val calculatedAt: Instant = Instant.now(),
)

data class TeamResultResponseDto(
    val id: UUID,
    val teamId: UUID,
    val formId: UUID,
    val averageScore: Double,
    val riskLevelDistribution: Map<RiskLevel, Int>,
    val calculatedAt: Instant,
) {
    companion object {
        fun fromDomain(tr: TeamResult) = TeamResultResponseDto(
            id = tr.id,
            teamId = tr.teamId,
            formId = tr.formId,
            averageScore = tr.averageScore,
            riskLevelDistribution = tr.riskLevelDistribution,
            calculatedAt = tr.calculatedAt,
        )
    }
}
