package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.FormRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.FormResponseDto
import com.psytrack.unformulieren.application.service.FormService
import com.psytrack.unformulieren.domain.enums.Status
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.Map

@Tag(name = "Forms")
@RestController
@RequestMapping("/forms")
class FormController(
    private val formService: FormService,
) {

    @Operation(summary = "Create form")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Form created"),
        ApiResponse(responseCode = "400", description = "Invalid data or duplicate title"),
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: FormRequestDto,
        authentication: Authentication,
    ): FormResponseDto =
        FormResponseDto.fromDomain(
            formService.create(request.title, request.description, request.status, authentication.name)
        )

    @Operation(summary = "List forms", description = "Returns forms scoped to the authenticated user's role.")
    @ApiResponse(responseCode = "200", description = "List of forms")
    @GetMapping
    fun list(
        @Parameter(description = "Filter by status: `ACTIVE` or `INACTIVE`")
        @RequestParam(required = false) status: Status?,
        authentication: Authentication,
    ): List<FormResponseDto> {
        val roles = authentication.authorities.map { it.authority }
        return when {
            roles.contains("ROLE_ADMIN") ->
                if (status != null) formService.findByStatus(status) else formService.list()
            roles.contains("ROLE_COUNSELOR") ->
                formService.listForCounselor(authentication.name)
            roles.contains("ROLE_EMPLOYEE") ->
                formService.findActiveForEmployee(authentication.name)
            else ->
                if (status != null) formService.findByStatus(status) else formService.list()
        }.map(FormResponseDto::fromDomain)
    }

    @Operation(summary = "Get form by title")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Form found"),
        ApiResponse(responseCode = "404", description = "Form not found"),
    )
    @GetMapping("/by-title")
    fun findByTitle(@Parameter(description = "Exact form title") @RequestParam title: String): FormResponseDto =
        FormResponseDto.fromDomain(
            formService.findByTitle(title).orElseThrow { IllegalArgumentException("Form not found with title: $title") }
        )

    @Operation(summary = "Get form by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Form found"),
        ApiResponse(responseCode = "404", description = "Form not found"),
    )
    @GetMapping("/{id}")
    fun get(@Parameter(description = "Form ID") @PathVariable id: UUID): FormResponseDto =
        FormResponseDto.fromDomain(formService.get(id))

    @Operation(summary = "Update form")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Form updated"),
        ApiResponse(responseCode = "404", description = "Form not found"),
    )
    @PutMapping("/{id}")
    fun update(
        @Parameter(description = "Form ID") @PathVariable id: UUID,
        @Valid @RequestBody request: FormRequestDto,
    ): FormResponseDto =
        FormResponseDto.fromDomain(
            formService.update(id, request.title, request.description, request.status, request.teamIds ?: emptyList())
        )

    @Operation(summary = "Close form")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Form closed and non-responders marked as no-response"),
        ApiResponse(responseCode = "403", description = "Only counselor/admin can close forms"),
    )
    @PutMapping("/{id}/close")
    fun close(
        @Parameter(description = "Form ID") @PathVariable id: UUID,
        authentication: Authentication,
    ): FormResponseDto {
        val roles = authentication.authorities.map { it.authority }
        if (!roles.contains("ROLE_COUNSELOR") && !roles.contains("ROLE_ADMIN")) {
            throw IllegalArgumentException("Only counselor or admin can close forms")
        }
        return FormResponseDto.fromDomain(formService.closeForm(id))
    }

    @Operation(summary = "Duplicate a form with a new name")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Form duplicated as a new draft"),
        ApiResponse(responseCode = "404", description = "Original form not found"),
    )
    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    fun duplicate(
        @Parameter(description = "Form ID to duplicate") @PathVariable id: UUID,
        @RequestBody body: Map<String, String>,
        authentication: Authentication,
    ): FormResponseDto {
        val newTitle = body["newTitle"] ?: throw IllegalArgumentException("newTitle is required")
        return FormResponseDto.fromDomain(formService.duplicate(id, newTitle, authentication.name))
    }

    @Operation(summary = "Delete form")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Form deleted"),
        ApiResponse(responseCode = "404", description = "Form not found"),
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@Parameter(description = "Form ID") @PathVariable id: UUID) {
        formService.delete(id)
    }
}
