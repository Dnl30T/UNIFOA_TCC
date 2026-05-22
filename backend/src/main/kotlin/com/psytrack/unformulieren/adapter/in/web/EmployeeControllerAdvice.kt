package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.ApiErrorDto
import com.psytrack.unformulieren.domain.exception.CounselorAlreadyHasTeamException
import com.psytrack.unformulieren.domain.exception.DuplicateTeamNameException
import com.psytrack.unformulieren.domain.exception.FormAlreadyAnsweredException
import com.psytrack.unformulieren.domain.exception.EmployeeNotFoundException
import com.psytrack.unformulieren.domain.exception.EmployeeResultNotFoundException
import com.psytrack.unformulieren.domain.exception.FormNotFoundException
import com.psytrack.unformulieren.domain.exception.FormResponseNotFoundException
import com.psytrack.unformulieren.domain.exception.ManagerAlreadyHasTeamException
import com.psytrack.unformulieren.domain.exception.QuestionNotFoundException
import com.psytrack.unformulieren.domain.exception.TeamNotFoundException
import com.psytrack.unformulieren.domain.exception.TeamResultNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class EmployeeControllerAdvice {

    @ExceptionHandler(
        EmployeeNotFoundException::class,
        TeamNotFoundException::class,
        FormNotFoundException::class,
        QuestionNotFoundException::class,
        FormResponseNotFoundException::class,
        EmployeeResultNotFoundException::class,
        TeamResultNotFoundException::class,
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(exception: RuntimeException): ApiErrorDto =
        ApiErrorDto(exception.message ?: "Resource not found")

    @ExceptionHandler(
        DuplicateTeamNameException::class,
        ManagerAlreadyHasTeamException::class,
        CounselorAlreadyHasTeamException::class,
        FormAlreadyAnsweredException::class,
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(exception: RuntimeException): ApiErrorDto =
        ApiErrorDto(exception.message ?: "Conflict")

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(exception: IllegalArgumentException): ApiErrorDto =
        ApiErrorDto(exception.message ?: "Invalid request")

    @ExceptionHandler(BadCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleBadCredentials(exception: BadCredentialsException): ApiErrorDto =
        ApiErrorDto(exception.message ?: "Invalid credentials")
}
