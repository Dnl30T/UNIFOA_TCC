package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.ClaimRoleRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.LoginRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.RegisterEmployeeRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.RegisterStaffRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.TokenResponseDto
import com.psytrack.unformulieren.application.service.AuthService
import com.psytrack.unformulieren.domain.enums.UserRole
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Authentication")
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    @Operation(summary = "Login", description = "Authenticates the user and returns a JWT token.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Login successful"),
        ApiResponse(responseCode = "401", description = "Invalid credentials"),
    )
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequestDto): TokenResponseDto =
        TokenResponseDto(authService.login(request.email, request.password))

    @Operation(
        summary = "Employee registration",
        description = "Creates an account with role `EMPLOYEE`. The team code is required and must match an existing team.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Account created and token returned"),
        ApiResponse(responseCode = "400", description = "Invalid data or team code not found"),
        ApiResponse(responseCode = "409", description = "Username already taken"),
    )
    @PostMapping("/register/employee")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerEmployee(@Valid @RequestBody request: RegisterEmployeeRequestDto): TokenResponseDto =
        TokenResponseDto(authService.registerEmployee(
            request.username, request.password, request.email, request.teamCode,
            request.name, request.phoneNumber, request.company, request.jobTitle,
        ))

    @Operation(
        summary = "Manager / Counselor registration",
        description = "Creates an account with role `PENDING`. After registration, call `POST /auth/claim-role` to set the definitive role (`MANAGER` or `COUNSELOR`).",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Account created and token returned"),
        ApiResponse(responseCode = "400", description = "Invalid data"),
        ApiResponse(responseCode = "409", description = "Username already taken"),
    )
    @PostMapping("/register/staff")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerStaff(@Valid @RequestBody request: RegisterStaffRequestDto): TokenResponseDto =
        TokenResponseDto(authService.registerStaff(
            request.username, request.password, request.email,
            request.name, request.phoneNumber, request.company, request.jobTitle,
        ))

    @Operation(
        summary = "Claim role (MANAGER or COUNSELOR)",
        description = "Only available to users with role `PENDING`. Sets the definitive role and returns a new JWT token with the updated role. Accepted values: `MANAGER`, `COUNSELOR`.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Role set and new token returned"),
        ApiResponse(responseCode = "400", description = "Invalid role (only MANAGER or COUNSELOR are accepted)"),
        ApiResponse(responseCode = "403", description = "User does not have PENDING role"),
    )
    @PostMapping("/claim-role")
    fun claimRole(
        @AuthenticationPrincipal username: String,
        @Valid @RequestBody request: ClaimRoleRequestDto,
    ): TokenResponseDto =
        TokenResponseDto(authService.claimRole(username, UserRole.valueOf(request.role.uppercase())))
}

