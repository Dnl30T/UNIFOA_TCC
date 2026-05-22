package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.EmployeeRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.EmployeeResponseDto
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort
import com.psytrack.unformulieren.application.service.EmployeeService
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Employees")
@RestController
@RequestMapping("/employees")
class EmployeeController(
    private val employeeService: EmployeeService,
    private val teamService: TeamService,
    private val userRepositoryPort: UserRepositoryPort,
) {

    @Operation(summary = "Hire employee", description = "Creates an employee record linking a user account to a team.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Employee created"),
        ApiResponse(responseCode = "400", description = "Invalid data"),
        ApiResponse(responseCode = "404", description = "User or team not found"),
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: EmployeeRequestDto): EmployeeResponseDto =
        EmployeeResponseDto.fromDomain(
            employeeService.hireEmployee(
                request.name,
                request.appUserId,
                request.teamId,
            ),
        )

    @Operation(summary = "List employees", description = "Returns employees visible to the authenticated user. Managers and Counselors only see their own team.")
    @ApiResponse(responseCode = "200", description = "List of employees")
    @GetMapping
    fun list(authentication: Authentication): List<EmployeeResponseDto> {
        val isManager = authentication.authorities.any { it.authority == "ROLE_MANAGER" }
        val isCounselor = authentication.authorities.any { it.authority == "ROLE_COUNSELOR" }
        val employees = if (isManager || isCounselor) {
            val appUser = userRepositoryPort.findByUsername(authentication.name)
                .orElseThrow { IllegalStateException("Authenticated user not found") }
            val team = if (isManager)
                teamService.findByManagerId(appUser.id)
            else
                teamService.findByCounselorId(appUser.id)
            team?.let { employeeService.listByTeam(it.id) } ?: emptyList()
        } else {
            employeeService.list()
        }
        return employees.map { employee ->
            val employeeAppUser = userRepositoryPort.findById(employee.appUserId).orElse(null)
            if (employeeAppUser?.isFullyAnonymized == true) {
                EmployeeResponseDto(employee.id, "An\u00f4nimo", employee.appUserId, employee.teamId, employee.status)
            } else {
                EmployeeResponseDto.fromDomain(employee)
            }
        }
    }

    @Operation(summary = "Get current employee", description = "Returns the employee record of the authenticated user.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Employee found"),
        ApiResponse(responseCode = "404", description = "No employee record for current user"),
    )
    @GetMapping("/me")
    fun me(authentication: Authentication): EmployeeResponseDto {
        val appUser = userRepositoryPort.findByUsername(authentication.name)
            .orElseThrow { IllegalStateException("Authenticated user not found") }
        val employee = employeeService.findByAppUserId(appUser.id)
            ?: throw com.psytrack.unformulieren.domain.exception.EmployeeNotFoundException(appUser.id)
        return EmployeeResponseDto.fromDomain(employee)
    }

    @Operation(summary = "Get employee by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Employee found"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
    )
    @GetMapping("/{id}")
    fun get(@Parameter(description = "Employee ID") @PathVariable id: UUID): EmployeeResponseDto =
        EmployeeResponseDto.fromDomain(employeeService.get(id))

    @Operation(summary = "Update employee")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Employee updated"),
        ApiResponse(responseCode = "400", description = "Invalid data"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
    )
    @PutMapping("/{id}")
    fun update(
        @Parameter(description = "Employee ID") @PathVariable id: UUID,
        @Valid @RequestBody request: EmployeeRequestDto,
    ): EmployeeResponseDto =
        EmployeeResponseDto.fromDomain(
            employeeService.update(
                id,
                request.name,
                request.teamId,
                request.status,
            ),
        )

    @Operation(summary = "Delete employee")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Employee deleted"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@Parameter(description = "Employee ID") @PathVariable id: UUID) {
        employeeService.delete(id)
    }
}