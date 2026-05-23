package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.EmployeeResultRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.EmployeeResultFinalizeRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.EmployeeResultResponseDto
import com.psytrack.unformulieren.application.service.EmployeeResultService
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
import java.time.Instant
import java.util.UUID

@Tag(name = "Employee Results")
@RestController
@RequestMapping("/employee-results")
class EmployeeResultController(
    private val employeeResultService: EmployeeResultService,
) {

    @Operation(summary = "Create employee result", description = "Records the calculated psychological health score for an employee on a form.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Result recorded"),
        ApiResponse(responseCode = "400", description = "Score out of range 0–100 or invalid data"),
        ApiResponse(responseCode = "404", description = "Employee or form not found"),
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: EmployeeResultRequestDto): EmployeeResultResponseDto =
        EmployeeResultResponseDto.fromDomain(
            employeeResultService.create(request.employeeId, request.formId, request.score, request.riskLevel, request.calculatedAt)
        )

    @Operation(
        summary = "List employee results",
        description = "Returns all results. Filter by `employeeId` or `formId` for specific results.",
    )
    @ApiResponse(responseCode = "200", description = "List of results")
    @GetMapping
    fun list(
        @Parameter(description = "Filter by employee ID") @RequestParam(required = false) employeeId: UUID?,
        @Parameter(description = "Filter by form ID") @RequestParam(required = false) formId: UUID?,
    ): List<EmployeeResultResponseDto> =
        when {
            employeeId != null -> listOf(EmployeeResultResponseDto.fromDomain(employeeResultService.getByEmployeeId(employeeId)))
            formId != null -> employeeResultService.listByFormId(formId).map(EmployeeResultResponseDto::fromDomain)
            else -> employeeResultService.list().map(EmployeeResultResponseDto::fromDomain)
        }

    @Operation(summary = "Get employee result by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Result found"),
        ApiResponse(responseCode = "404", description = "Result not found"),
    )
    @GetMapping("/{id}")
    fun get(@Parameter(description = "Result ID") @PathVariable id: UUID): EmployeeResultResponseDto =
        EmployeeResultResponseDto.fromDomain(employeeResultService.get(id))

    @Operation(summary = "Update employee result")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Result updated"),
        ApiResponse(responseCode = "404", description = "Result not found"),
    )
    @PutMapping("/{id}")
    fun update(
        @Parameter(description = "Result ID") @PathVariable id: UUID,
        @Valid @RequestBody request: EmployeeResultRequestDto,
    ): EmployeeResultResponseDto =
        EmployeeResultResponseDto.fromDomain(
            employeeResultService.update(id, request.employeeId, request.formId, request.score, request.riskLevel, request.calculatedAt)
        )

    @Operation(summary = "Finalize employee analysis score", description = "Allows counselor to manually set the final employee score while keeping helper score as advisory.")
    @PutMapping("/finalize")
    fun finalizeScore(@Valid @RequestBody request: EmployeeResultFinalizeRequestDto): EmployeeResultResponseDto =
        EmployeeResultResponseDto.fromDomain(
            employeeResultService.setFinalScore(request.employeeId, request.formId, request.finalScore, Instant.now())
        )

    @Operation(summary = "Delete employee result")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Result deleted"),
        ApiResponse(responseCode = "404", description = "Result not found"),
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@Parameter(description = "Result ID") @PathVariable id: UUID) {
        employeeResultService.delete(id)
    }
}
