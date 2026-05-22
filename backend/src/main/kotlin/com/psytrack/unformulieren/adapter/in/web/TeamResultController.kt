package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.TeamResultRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.TeamResultResponseDto
import com.psytrack.unformulieren.application.service.TeamResultService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Team Results")
@RestController
@RequestMapping("/team-results")
class TeamResultController(
    private val teamResultService: TeamResultService,
) {

    @Operation(summary = "Create team result", description = "Records the consolidated psychological health result for a team on a form.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Result recorded"),
        ApiResponse(responseCode = "400", description = "Invalid data"),
        ApiResponse(responseCode = "404", description = "Team or form not found"),
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: TeamResultRequestDto): TeamResultResponseDto =
        TeamResultResponseDto.fromDomain(
            teamResultService.create(request.teamId, request.formId, request.averageScore, request.riskLevelDistribution, request.calculatedAt)
        )

    @Operation(
        summary = "List team results",
        description = "Returns all results. Filter by `teamId` or `formId` for specific results.",
    )
    @ApiResponse(responseCode = "200", description = "List of results")
    @GetMapping
    fun list(
        @Parameter(description = "Filter by team ID") @RequestParam(required = false) teamId: UUID?,
        @Parameter(description = "Filter by form ID") @RequestParam(required = false) formId: UUID?,
    ): List<TeamResultResponseDto> =
        when {
            teamId != null -> listOf(TeamResultResponseDto.fromDomain(teamResultService.getByTeamId(teamId)))
            formId != null -> listOf(TeamResultResponseDto.fromDomain(teamResultService.getByFormId(formId)))
            else -> teamResultService.list().map(TeamResultResponseDto::fromDomain)
        }

    @Operation(summary = "Get team result by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Result found"),
        ApiResponse(responseCode = "404", description = "Result not found"),
    )
    @GetMapping("/{id}")
    fun get(@Parameter(description = "Result ID") @PathVariable id: UUID): TeamResultResponseDto =
        TeamResultResponseDto.fromDomain(teamResultService.get(id))

    @Operation(summary = "Update team result")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Result updated"),
        ApiResponse(responseCode = "404", description = "Result not found"),
    )
    @PutMapping("/{id}")
    fun update(
        @Parameter(description = "Result ID") @PathVariable id: UUID,
        @Valid @RequestBody request: TeamResultRequestDto,
    ): TeamResultResponseDto =
        TeamResultResponseDto.fromDomain(
            teamResultService.update(id, request.teamId, request.formId, request.averageScore, request.riskLevelDistribution, request.calculatedAt)
        )

    @Operation(summary = "Delete team result")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Result deleted"),
        ApiResponse(responseCode = "404", description = "Result not found"),
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@Parameter(description = "Result ID") @PathVariable id: UUID) {
        teamResultService.delete(id)
    }
}
