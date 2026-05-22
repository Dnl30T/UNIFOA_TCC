package com.psytrack.unformulieren.adapter.`in`.web.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FullNameValidator::class])
annotation class FullName(
    val message: String = "Full name must contain at least a first name and a last name",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class FullNameValidator : ConstraintValidator<FullName, String?> {

    // Accepts two or more words made of letters (including accented), hyphens and apostrophes,
    // separated by single spaces. Allows null (use @NotBlank separately to enforce presence).
    private val pattern = Regex(
        """^[A-Za-zÀ-ÖØ-öø-ÿ''\-]+([ ][A-Za-zÀ-ÖØ-öø-ÿ''\-]+)+$"""
    )

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        return pattern.matches(value.trim())
    }
}
