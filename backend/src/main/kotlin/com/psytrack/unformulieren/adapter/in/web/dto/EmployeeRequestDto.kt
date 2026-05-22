package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.enums.EmployeeStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class EmployeeRequestDto(
    @field:NotBlank val name: String,
    @field:NotNull val appUserId: UUID,
    @field:NotNull val teamId: UUID,
    val status: EmployeeStatus? = null,
)