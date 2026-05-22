package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.FormResponseResponseDto
import com.psytrack.unformulieren.application.service.FormResponseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Form Responses")
@RestController
@RequestMapping("/form-responses")
class FormResponseController(
    private val formResponseService: FormResponseService,
) {

    @Operation(
        summary = "List submissions",
        description = "Returns all submissions. Filter by employeeId or formId.",
    )
    @GetMapping
    fun list(
        @Parameter(description = "Filter by employee ID") @RequestParam(required = false) employeeId: UUID?,
        @Parameter(description = "Filter by form ID") @RequestParam(required = false) formId: UUID?,
    ): List<FormResponseResponseDto> = when {
        employeeId != null -> formResponseService.findByEmployeeId(employeeId).map(FormResponseResponseDto::fromDomain)
        formId != null -> formResponseService.findByFormId(formId).map(FormResponseResponseDto::fromDomain)
        else -> formResponseService.list().map(FormResponseResponseDto::fromDomain)
    }

    @Operation(summary = "Get submission by form and employee")
    @GetMapping("/by-form-and-employee")
    fun getByFormAndEmployee(
        @RequestParam formId: UUID,
        @RequestParam employeeId: UUID,
    ): FormResponseResponseDto =
        FormResponseResponseDto.fromDomain(formResponseService.getByFormAndEmployee(formId, employeeId))
}
