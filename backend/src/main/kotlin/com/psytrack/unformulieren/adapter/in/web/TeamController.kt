package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.TeamRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.TeamResponseDto
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort
import com.psytrack.unformulieren.application.service.TeamService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Tag(name = "Teams")
@RestController
@RequestMapping("/teams")
class TeamController(
    private val teamService: TeamService,
    private val userRepositoryPort: UserRepositoryPort,
) {

    @Operation(summary = "Create team", description = "Creates a new team and automatically generates a unique invite code.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Team created"),
        ApiResponse(responseCode = "400", description = "Invalid data or duplicate team name"),
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: TeamRequestDto, authentication: Authentication): TeamResponseDto {
        val manager = userRepositoryPort.findByUsername(authentication.name)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        return TeamResponseDto.fromDomain(teamService.registerTeam(request.name, manager.id))
    }

    @Operation(summary = "Join team as counselor", description = "Associates the authenticated counselor with the team identified by the invite code.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Joined successfully"),
        ApiResponse(responseCode = "404", description = "Team not found"),
        ApiResponse(responseCode = "409", description = "Counselor is already assigned to another team"),
    )
    @PatchMapping("/join")
    fun joinAsCounselor(
        @Parameter(description = "Team invite code") @RequestParam code: String,
        authentication: Authentication,
    ): TeamResponseDto {
        val counselor = userRepositoryPort.findByUsername(authentication.name)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val team = teamService.findByCode(code)
        return TeamResponseDto.fromDomain(teamService.assignCounselor(team.id, counselor.id))
    }

    @Operation(summary = "Get my team", description = "Returns the team for the authenticated manager or counselor.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Team found"),
        ApiResponse(responseCode = "404", description = "No team found for this user"),
    )
    @GetMapping("/my")
    fun getMyTeam(authentication: Authentication): TeamResponseDto {
        val user = userRepositoryPort.findByUsername(authentication.name)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val isCounselor = authentication.authorities.any { it.authority == "ROLE_COUNSELOR" }
        val team = if (isCounselor)
            teamService.findByCounselorId(user.id)
        else
            teamService.findByManagerId(user.id)
        team ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No team found for this user")
        return TeamResponseDto.fromDomain(team)
    }

    @Operation(summary = "List teams")
    @ApiResponse(responseCode = "200", description = "List of teams")
    @GetMapping
    fun list(authentication: Authentication): List<TeamResponseDto> {
        val isCounselor = authentication.authorities.any { it.authority == "ROLE_COUNSELOR" }
        if (isCounselor) {
            val user = userRepositoryPort.findByUsername(authentication.name)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
            val team = teamService.findByCounselorId(user.id) ?: return emptyList()
            return listOf(TeamResponseDto.fromDomain(team))
        }
        return teamService.list().map(TeamResponseDto::fromDomain)
    }

    @Operation(summary = "Get team by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Team found"),
        ApiResponse(responseCode = "404", description = "Team not found"),
    )
    @GetMapping("/{id}")
    fun get(@Parameter(description = "Team ID") @PathVariable id: UUID): TeamResponseDto =
        TeamResponseDto.fromDomain(teamService.get(id))

    @Operation(summary = "Find team by invite code")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Team found"),
        ApiResponse(responseCode = "404", description = "Team not found"),
    )
    @GetMapping("/by-code")
    fun findByCode(
        @Parameter(description = "Team invite code") @RequestParam code: String
    ): TeamResponseDto =
        TeamResponseDto.fromDomain(teamService.findByCode(code))

    @Operation(summary = "Update team")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Team updated"),
        ApiResponse(responseCode = "404", description = "Team not found"),
    )
    @PutMapping("/{id}")
    fun update(
        @Parameter(description = "Team ID") @PathVariable id: UUID,
        @Valid @RequestBody request: TeamRequestDto,
    ): TeamResponseDto =
        TeamResponseDto.fromDomain(teamService.update(id, request.name))

    @Operation(summary = "Delete team")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Team deleted"),
        ApiResponse(responseCode = "404", description = "Team not found"),
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@Parameter(description = "Team ID") @PathVariable id: UUID) {
        teamService.delete(id)
    }
}
