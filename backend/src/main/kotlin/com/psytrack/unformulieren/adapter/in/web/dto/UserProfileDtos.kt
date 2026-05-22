package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.adapter.`in`.web.validation.FullName

data class ProfileUpdateRequestDto(
    @field:FullName val name: String?,
    val company: String?,
    val jobTitle: String?,
)

data class ProfileResponseDto(
    val name: String?,
    val email: String,
    val company: String?,
    val jobTitle: String?,
)

data class PasswordChangeRequestDto(
    val currentPassword: String,
    val newPassword: String,
)
