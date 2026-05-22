package com.psytrack.unformulieren.adapter.`in`.web

import com.psytrack.unformulieren.adapter.`in`.web.dto.PasswordChangeRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.PrivacyPreferencesRequestDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.PrivacyPreferencesResponseDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.ProfileResponseDto
import com.psytrack.unformulieren.adapter.`in`.web.dto.ProfileUpdateRequestDto
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort
import com.psytrack.unformulieren.application.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

data class ProfilePictureResponseDto(val profilePictureUrl: String)

@Tag(name = "Users")
@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userService: UserService,
    private val userRepositoryPort: UserRepositoryPort,
) {

    @Operation(
        summary = "Upload profile picture",
        description = "Uploads a profile picture (JPEG, PNG or WebP, max 5 MB) for the authenticated user. " +
                "This endpoint is optional — users may skip it entirely.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Picture uploaded successfully"),
        ApiResponse(responseCode = "400", description = "Invalid file type or size"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
    )
    @PatchMapping("/me/profile-picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfilePicture(
        @RequestPart("file") file: MultipartFile,
        authentication: Authentication,
    ): ProfilePictureResponseDto {
        val contentType = file.contentType ?: "application/octet-stream"
        val url = userService.uploadProfilePicture(
            authentication.name,
            file.bytes,
            contentType,
        )
        return ProfilePictureResponseDto(url)
    }

    @Operation(
        summary = "Remove profile picture",
        description = "Clears the profile picture URL for the authenticated user.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Picture removed"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
    )
    @DeleteMapping("/me/profile-picture")
    fun removeProfilePicture(authentication: Authentication) {
        userService.removeProfilePicture(authentication.name)
    }

    @Operation(summary = "Get privacy preferences")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Current privacy preferences"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
    )
    @GetMapping("/me/privacy")
    fun getPrivacy(authentication: Authentication): PrivacyPreferencesResponseDto {
        val user = userRepositoryPort.findByUsername(authentication.name)
            .orElseThrow { IllegalStateException("Authenticated user not found") }
        return PrivacyPreferencesResponseDto(user.isFullyAnonymized)
    }

    @Operation(summary = "Update privacy preferences")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Privacy preferences updated"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
    )
    @PutMapping("/me/privacy")
    fun updatePrivacy(
        @RequestBody request: PrivacyPreferencesRequestDto,
        authentication: Authentication,
    ): PrivacyPreferencesResponseDto {
        val updated = userService.updatePrivacyPreferences(authentication.name, request.fullyAnonymized)
        return PrivacyPreferencesResponseDto(updated)
    }

    @Operation(summary = "Get current user profile")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Profile returned"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
    )
    @GetMapping("/me/profile")
    fun getProfile(authentication: Authentication): ProfileResponseDto {
        val user = userRepositoryPort.findByUsername(authentication.name)
            .orElseThrow { IllegalStateException("Authenticated user not found") }
        return ProfileResponseDto(user.name, user.email, user.company, user.jobTitle)
    }

    @Operation(summary = "Update current user profile")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Profile updated"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
    )
    @PutMapping("/me/profile")
    fun updateProfile(
        @RequestBody request: ProfileUpdateRequestDto,
        authentication: Authentication,
    ): ProfileResponseDto {
        val user = userService.updateProfileDetails(
            authentication.name, request.name, request.company, request.jobTitle)
        return ProfileResponseDto(user.name, user.email, user.company, user.jobTitle)
    }

    @Operation(summary = "Change password")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Password changed"),
        ApiResponse(responseCode = "400", description = "Current password incorrect"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
    )
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        @RequestBody request: PasswordChangeRequestDto,
        authentication: Authentication,
    ) {
        try {
            userService.changePassword(authentication.name, request.currentPassword, request.newPassword)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
    }
}
