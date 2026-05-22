package com.psytrack.unformulieren.adapter.`in`.web.dto

import com.psytrack.unformulieren.adapter.`in`.web.validation.FullName
import com.psytrack.unformulieren.adapter.`in`.web.validation.StrongPassword
import jakarta.validation.constraints.NotBlank

data class LoginRequestDto(
	@field:NotBlank val email: String,
	@field:NotBlank val password: String,
)

data class RegisterEmployeeRequestDto(
	@field:NotBlank val username: String,
	@field:NotBlank @field:StrongPassword val password: String,
	@field:NotBlank val email: String,
	@field:NotBlank val teamCode: String,
	@field:NotBlank @field:FullName val name: String,
	val phoneNumber: String? = null,
	val company: String? = null,
	val jobTitle: String? = null,
)

data class RegisterStaffRequestDto(
	@field:NotBlank val username: String,
	@field:NotBlank @field:StrongPassword val password: String,
	@field:NotBlank val email: String,
	@field:NotBlank @field:FullName val name: String,
	val phoneNumber: String? = null,
	val company: String? = null,
	val jobTitle: String? = null,
)

data class ClaimRoleRequestDto(
	@field:NotBlank val role: String,
)

data class TokenResponseDto(val token: String)
