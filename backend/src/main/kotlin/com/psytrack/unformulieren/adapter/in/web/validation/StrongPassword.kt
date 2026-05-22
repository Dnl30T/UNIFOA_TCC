package com.psytrack.unformulieren.adapter.`in`.web.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [StrongPasswordValidator::class])
annotation class StrongPassword(
    val message: String = "Password must be at least 8 characters and contain uppercase, lowercase, digit and special character",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class StrongPasswordValidator : ConstraintValidator<StrongPassword, String> {

    private val pattern = Regex(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}\$"
    )

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return false
        return pattern.matches(value)
    }
}
