package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.FormSubmissionRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.FormSubmissionResponseDto
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort
import com.psytrack.unformulieren.application.service.EmployeeResultService
import com.psytrack.unformulieren.application.service.FormService
import com.psytrack.unformulieren.application.service.EmployeeService
import com.psytrack.unformulieren.application.service.FormResponseService
import com.psytrack.unformulieren.application.service.TeamService
import com.psytrack.unformulieren.domain.enums.Status
import com.psytrack.unformulieren.domain.enums.QuestionType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@Tag(name = "Form Submissions")
@RestController
@RequestMapping("/form-submissions")
class FormSubmissionController(
    private val formResponseService: FormResponseService,
    private val formService: FormService,
    private val employeeResultService: EmployeeResultService,
    private val teamService: TeamService,
    private val employeeService: EmployeeService,
    private val userRepositoryPort: UserRepositoryPort,
) {

    @Operation(summary = "Submit a complete form", description = "Submits all answers for a form in a single call. Returns 409 if already submitted.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun submit(@Valid @RequestBody request: FormSubmissionRequestDto): FormSubmissionResponseDto {
        val form = formService.get(request.formId)
        if (form.status == Status.ENDED) {
            throw IllegalArgumentException("Form is closed and no longer accepts responses")
        }

        val now = Instant.now()
        val numericAnswers = request.answers
            .filter { it.textValue == null }
            .associate { it.questionId to it.value }
        val textAnswers = request.answers
            .filter { it.textValue != null }
            .associate { it.questionId to it.textValue!! }

        val answeredQuestionIds = numericAnswers.keys + textAnswers.keys
        val missingRequired = form.questions
            .filter { it.isRequired }
            .filter { !answeredQuestionIds.contains(it.id) || isMissingTextAnswer(it.type, textAnswers[it.id]) }
        if (missingRequired.isNotEmpty()) {
            throw IllegalArgumentException("Required questions are missing in the submitted form")
        }

        val saved = formResponseService.submitBatch(
            request.formId, request.employeeId, numericAnswers, textAnswers, now)

        val result = employeeResultService.findByEmployeeIdAndFormId(request.employeeId, request.formId).orElse(null)
        return FormSubmissionResponseDto(
            saved.formId,
            saved.employeeId,
            saved.answers.size + saved.textAnswers.size,
            saved.submittedAt,
            saved.status,
            result?.helperScore,
            result?.finalScore,
            result?.riskLevel,
        )
    }

    @Operation(summary = "List submissions", description = "Returns submissions filtered by employeeId or formId. COUNSELOR without filters sees only own team's submissions.")
    @GetMapping
    fun list(
        authentication: Authentication,
        @RequestParam(required = false) employeeId: UUID?,
        @RequestParam(required = false) formId: UUID?,
    ): List<FormSubmissionResponseDto> {
        val responses = when {
            employeeId != null -> formResponseService.findByEmployeeId(employeeId)
            formId != null -> {
                val all = formResponseService.findByFormId(formId)
                val isCounselor = authentication.authorities.any { it.authority == "ROLE_COUNSELOR" }
                if (isCounselor) {
                    val teamEmployeeIds = getCounselorTeamEmployeeIds(authentication)
                    all.filter { teamEmployeeIds.contains(it.employeeId) }
                } else all
            }
            else -> {
                val isCounselor = authentication.authorities.any { it.authority == "ROLE_COUNSELOR" }
                if (isCounselor) {
                    val teamEmployeeIds = getCounselorTeamEmployeeIds(authentication)
                    formResponseService.list().filter { teamEmployeeIds.contains(it.employeeId) }
                } else formResponseService.list()
            }
        }
        return responses.map { r ->
            val result = employeeResultService.findByEmployeeIdAndFormId(r.employeeId, r.formId).orElse(null)
            FormSubmissionResponseDto(
                r.formId,
                r.employeeId,
                r.answers.size + r.textAnswers.size,
                r.submittedAt,
                r.status,
                result?.helperScore,
                result?.finalScore,
                result?.riskLevel,
            )
        }
    }

    private fun isMissingTextAnswer(type: QuestionType, value: String?): Boolean {
        if (type != QuestionType.TEXT && type != QuestionType.DATE) {
            return false
        }
        return value.isNullOrBlank()
    }

    private fun getCounselorTeamEmployeeIds(authentication: Authentication): Set<UUID> {
        val appUser = userRepositoryPort.findByUsername(authentication.name).orElse(null) ?: return emptySet()
        val team = teamService.findByCounselorId(appUser.id) ?: return emptySet()
        return employeeService.listByTeam(team.id).map { it.id }.toSet()
    }
}
