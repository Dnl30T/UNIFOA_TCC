package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.QuestionRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.QuestionResponseDto
import com.psytrack.unformulieren.application.service.QuestionService
import com.psytrack.unformulieren.domain.enums.QuestionType
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

@Tag(name = "Questions")
@RestController
@RequestMapping("/questions")
class QuestionController(
    private val questionService: QuestionService,
) {

    @Operation(summary = "Create question")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Question created"),
        ApiResponse(responseCode = "400", description = "Invalid data"),
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: QuestionRequestDto): QuestionResponseDto =
        QuestionResponseDto.fromDomain(
            questionService.create(request.formId, request.text, request.type, request.required, request.config, request.order)
        )

    @Operation(summary = "List questions", description = "Returns all questions. Filter by `type` if needed.")
    @ApiResponse(responseCode = "200", description = "List of questions")
    @GetMapping
    fun list(
        @Parameter(description = "Filter by question type") @RequestParam(required = false) type: QuestionType?,
    ): List<QuestionResponseDto> =
        if (type != null) questionService.findByType(type).map(QuestionResponseDto::fromDomain)
        else questionService.list().map(QuestionResponseDto::fromDomain)

    @Operation(summary = "Get question by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Question found"),
        ApiResponse(responseCode = "404", description = "Question not found"),
    )
    @GetMapping("/{id}")
    fun get(@Parameter(description = "Question ID") @PathVariable id: UUID): QuestionResponseDto =
        QuestionResponseDto.fromDomain(questionService.get(id))

    @Operation(summary = "Update question")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Question updated"),
        ApiResponse(responseCode = "404", description = "Question not found"),
    )
    @PutMapping("/{id}")
    fun update(
        @Parameter(description = "Question ID") @PathVariable id: UUID,
        @Valid @RequestBody request: QuestionRequestDto,
    ): QuestionResponseDto =
        QuestionResponseDto.fromDomain(
            questionService.update(id, request.formId, request.text, request.type, request.required, request.config, request.order)
        )

    @Operation(summary = "Delete question")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Question deleted"),
        ApiResponse(responseCode = "404", description = "Question not found"),
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@Parameter(description = "Question ID") @PathVariable id: UUID) {
        questionService.delete(id)
    }
}
