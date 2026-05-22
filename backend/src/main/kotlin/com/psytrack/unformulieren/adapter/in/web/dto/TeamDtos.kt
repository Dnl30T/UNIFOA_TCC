package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.model.Team
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class TeamRequestDto(
    @field:NotBlank val name: String,
)

data class TeamResponseDto(val id: UUID, val name: String, val teamCode: String?) {
    companion object {
        fun fromDomain(team: Team) = TeamResponseDto(id = team.id, name = team.name, teamCode = team.teamCode)
    }
}
