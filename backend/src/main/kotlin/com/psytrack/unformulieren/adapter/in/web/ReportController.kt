package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.application.service.ReportService
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort
import com.psytrack.unformulieren.application.service.EmployeeService
import com.psytrack.unformulieren.application.service.TeamService
import com.psytrack.unformulieren.domain.enums.RiskLevel
import com.psytrack.unformulieren.domain.model.Report
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class ReportGenerateRequestDto(
    val formId: UUID,
    val teamId: UUID,
    val name: String,
)

data class ReportResponseDto(
    val id: UUID,
    val formId: UUID,
    val teamId: UUID,
    val name: String,
    val respondentIds: List<UUID>,
    val respondentCount: Int,
    val averageScore: Double,
    val generalRisk: RiskLevel,
    val riskDistribution: Map<String, Int>,
    val generatedAt: Instant,
    val createdBy: String,
)

@Tag(name = "Reports")
@RestController
@RequestMapping("/reports")
class ReportController(
    private val reportService: ReportService,
    private val userRepositoryPort: UserRepositoryPort,
    private val employeeService: EmployeeService,
    private val teamService: TeamService,
) {

    @Operation(summary = "Generate a report for a closed form (counselor/admin only)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun generate(
        @RequestBody body: ReportGenerateRequestDto,
        authentication: Authentication,
    ): ReportResponseDto {
        val report = reportService.generate(body.formId, body.teamId, body.name, authentication.name)
        return report.toDto()
    }

    @Operation(summary = "List reports by teamId or formId")
    @GetMapping
    fun list(
        @RequestParam(required = false) teamId: UUID?,
        @RequestParam(required = false) formId: UUID?,
        authentication: Authentication,
    ): List<ReportResponseDto> {
        val isCounselorOrAdmin = authentication.authorities.any {
            it.authority == "ROLE_COUNSELOR" || it.authority == "ROLE_ADMIN"
        }

        return when {
            formId != null -> reportService.findByFormId(formId).map { it.toDto() }
            teamId != null -> reportService.findByTeamId(teamId).map { it.toDto() }
            // Manager: scope to own team
            !isCounselorOrAdmin -> {
                val appUser = userRepositoryPort.findByUsername(authentication.name).orElse(null)
                val teamId2 = appUser?.let { teamService.findByManagerId(it.id)?.id }
                teamId2?.let { reportService.findByTeamId(it).map { r -> r.toDto() } } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    @Operation(summary = "Get a specific report by teamId and reportId")
    @GetMapping("/{teamId}/{reportId}")
    fun get(
        @PathVariable teamId: UUID,
        @PathVariable reportId: UUID,
    ): ResponseEntity<ReportResponseDto> =
        reportService.findById(teamId, reportId)
            .map { ResponseEntity.ok(it.toDto()) }
            .orElse(ResponseEntity.notFound().build())
}

private fun Report.toDto() = ReportResponseDto(
    id = id,
    formId = formId,
    teamId = teamId,
    name = name,
    respondentIds = respondentIds,
    respondentCount = respondentCount,
    averageScore = averageScore,
    generalRisk = generalRisk,
    riskDistribution = riskDistribution.entries.associate { it.key.name to it.value },
    generatedAt = generatedAt,
    createdBy = createdBy,
)
