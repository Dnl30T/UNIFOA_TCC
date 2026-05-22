package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.domain.enums.EmployeeStatus
import com.psytrack.unformulieren.domain.model.Employee
import java.util.UUID

data class EmployeeResponseDto(
    val id: UUID,
    val name: String,
    val appUserId: UUID,
    val teamId: UUID,
    val status: EmployeeStatus,
) {
    companion object {
        fun fromDomain(employee: Employee): EmployeeResponseDto = EmployeeResponseDto(
            id = employee.id,
            name = employee.name,
            appUserId = employee.appUserId,
            teamId = employee.teamId,
            status = employee.status,
        )
    }
}