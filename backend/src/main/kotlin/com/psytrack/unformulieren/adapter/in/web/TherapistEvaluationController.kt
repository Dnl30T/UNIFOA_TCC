package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.TherapistEvaluationRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.TherapistComprehensiveResponseDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.TherapistEvaluationResponseDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.PublishBatchRequestDto
import com.psytrack.unformulieren.application.service.EmployeeResultService
import com.psytrack.unformulieren.application.service.FormResponseService
import com.psytrack.unformulieren.application.service.TherapistEvaluationService
import com.psytrack.unformulieren.application.service.EmployeeService
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort
import com.psytrack.unformulieren.domain.enums.TherapistEvaluationStatus
import com.psytrack.unformulieren.domain.model.TherapistEvaluation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Therapist Evaluations")
@RestController
@RequestMapping("/therapist-evaluations")
class TherapistEvaluationController(
    private val service: TherapistEvaluationService,
    private val formResponseService: FormResponseService,
    private val employeeResultService: EmployeeResultService,
    private val employeeService: EmployeeService,
    private val userRepositoryPort: UserRepositoryPort,
) {

    @Operation(summary = "Create or update a therapist evaluation")
    @PutMapping("/{formId}/{employeeId}")
    fun upsert(
        @PathVariable formId: UUID,
        @PathVariable employeeId: UUID,
        @RequestBody body: TherapistEvaluationRequestDto,
        authentication: Authentication,
    ): TherapistEvaluationResponseDto {
        val evaluation = TherapistEvaluation(
            formId, employeeId,
            body.occupationalContext, body.teamDynamics,
            body.psychosomaticSymptoms, body.cognitiveEmotionalChanges,
            body.exhaustionScore, body.exhaustionJustification,
            body.depersonalizationScore, body.depersonalizationJustification,
            body.differentialDiagnosis, body.interventionPlan,
            body.internalNote, body.hrSummary,
            body.closingCommentary,
            body.stressScore, body.sleepScore, body.overloadScore,
            body.fatigueScore, body.disengagementScore, body.isolationScore,
            body.status ?: TherapistEvaluationStatus.DRAFT,
            null,
            authentication.name, null, null,
        )
        return service.upsert(evaluation).toDto(includePrivate = true)
    }

    @Operation(summary = "Get a therapist evaluation")
    @GetMapping("/{formId}/{employeeId}")
    fun get(
        @PathVariable formId: UUID,
        @PathVariable employeeId: UUID,
        authentication: Authentication,
    ): ResponseEntity<TherapistEvaluationResponseDto> {
        val isCounselorOrAdmin = authentication.authorities.any {
            it.authority == "ROLE_COUNSELOR" || it.authority == "ROLE_ADMIN"
        }
        return service.find(formId, employeeId)
            .map { ResponseEntity.ok(it.toDto(includePrivate = isCounselorOrAdmin)) }
            .orElse(ResponseEntity.notFound().build())
    }

    @Operation(summary = "Get therapist comprehensive form view")
    @GetMapping("/{formId}/{employeeId}/comprehensive")
    fun getComprehensive(
        @PathVariable formId: UUID,
        @PathVariable employeeId: UUID,
        authentication: Authentication,
    ): TherapistComprehensiveResponseDto {
        val isCounselorOrAdmin = authentication.authorities.any {
            it.authority == "ROLE_COUNSELOR" || it.authority == "ROLE_ADMIN"
        }
        val response = formResponseService.getByFormAndEmployee(formId, employeeId)
        val result = employeeResultService.findByEmployeeIdAndFormId(employeeId, formId).orElse(null)
        val evaluation = service.find(formId, employeeId).orElse(null)

        return TherapistComprehensiveResponseDto(
            formId = formId,
            employeeId = employeeId,
            answeredQuestions = response.answers.size,
            textQuestions = response.textAnswers.size,
            responseStatus = response.status,
            helperScore = result?.helperScore,
            finalScore = result?.finalScore,
            burnoutRiskPreAnalysis = result?.riskLevel,
            submittedAt = response.submittedAt,
            closedAt = response.closedAt,
            therapistEvaluation = evaluation?.toDto(includePrivate = isCounselorOrAdmin),
        )
    }

    @Operation(summary = "Publish one employee analysis")
    @PostMapping("/{formId}/{employeeId}/publish")
    fun publish(
        @PathVariable formId: UUID,
        @PathVariable employeeId: UUID,
    ): TherapistEvaluationResponseDto =
        service.publish(formId, employeeId).toDto(includePrivate = true)

    @Operation(summary = "Batch publish employee analyses")
    @PostMapping("/{formId}/publish-batch")
    fun publishBatch(
        @PathVariable formId: UUID,
        @RequestBody body: PublishBatchRequestDto,
    ): List<TherapistEvaluationResponseDto> =
        service.publishBatch(formId, body.employeeIds)
            .map { it.toDto(includePrivate = true) }

    @Operation(summary = "List all evaluations for a form (counselor/admin only)")
    @GetMapping("/form/{formId}")
    fun listByForm(
        @PathVariable formId: UUID,
    ): List<TherapistEvaluationResponseDto> =
        service.findAllByFormId(formId).map { it.toDto(includePrivate = true) }

    @Operation(summary = "List published evaluations for a form")
    @GetMapping("/form/{formId}/published")
    fun listPublishedByForm(
        @PathVariable formId: UUID,
    ): List<TherapistEvaluationResponseDto> =
        service.findPublishedByFormId(formId).map { it.toDto(includePrivate = false) }

    @Operation(summary = "Get published analysis for current employee")
    @GetMapping("/{formId}/employee-view")
    fun employeeView(
        @PathVariable formId: UUID,
        authentication: Authentication,
    ): ResponseEntity<TherapistEvaluationResponseDto> {
        val appUser = userRepositoryPort.findByUsername(authentication.name).orElse(null) ?: return ResponseEntity.notFound().build()
        val employee = employeeService.findByAppUserId(appUser.id) ?: return ResponseEntity.notFound().build()
        return service.findPublished(formId, employee.id)
            .map { ResponseEntity.ok(it.toDto(includePrivate = false)) }
            .orElse(ResponseEntity.notFound().build())
    }
}

private fun TherapistEvaluation.toDto(includePrivate: Boolean) = TherapistEvaluationResponseDto(
    formId = formId,
    employeeId = employeeId,
    occupationalContext = occupationalContext,
    teamDynamics = teamDynamics,
    psychosomaticSymptoms = psychosomaticSymptoms,
    cognitiveEmotionalChanges = cognitiveEmotionalChanges,
    exhaustionScore = exhaustionScore,
    exhaustionJustification = exhaustionJustification,
    depersonalizationScore = depersonalizationScore,
    depersonalizationJustification = depersonalizationJustification,
    differentialDiagnosis = differentialDiagnosis,
    interventionPlan = interventionPlan,
    internalNote = if (includePrivate) internalNote else null,
    hrSummary = hrSummary,
    closingCommentary = closingCommentary,
    stressScore = stressScore,
    sleepScore = sleepScore,
    overloadScore = overloadScore,
    fatigueScore = fatigueScore,
    disengagementScore = disengagementScore,
    isolationScore = isolationScore,
    status = status,
    publishedAt = publishedAt,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
