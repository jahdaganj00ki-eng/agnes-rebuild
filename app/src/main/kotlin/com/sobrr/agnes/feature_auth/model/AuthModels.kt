package com.sobrr.agnes.feature_auth.model

import com.google.gson.annotations.SerializedName
import com.sobrr.agnes.data.model.base.BaseResponse

// Auth request/response DTOs
data class AuthEmailRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthPhoneRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("code") val code: String
)

data class AuthGoogleRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("access_token") val accessToken: String?
)

data class RegisterByEmailRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("username") val username: String?,
    @SerializedName("invitation_code") val invitationCode: String?
)

data class BindEmailRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String
)

data class BindPhoneRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("code") val code: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("code") val code: String
)

data class ChangeAvatarRequest(
    @SerializedName("avatar_url") val avatarUrl: String
)

data class ChangeUserNameRequest(
    @SerializedName("username") val username: String
)

data class UpdateProfileRequest(
    @SerializedName("username") val username: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("bio") val bio: String?
)

data class TimezoneRequest(
    @SerializedName("timezone") val timezone: String
)

data class SendCodeRequest(
    @SerializedName("contact") val contact: String,
    @SerializedName("type") val type: String // "email" or "phone"
)

data class VerifyCodeRequest(
    @SerializedName("contact") val contact: String,
    @SerializedName("code") val code: String,
    @SerializedName("type") val type: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("token_type") val tokenType: String = "Bearer"
)

data class AuthMeUserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class OwnerProfileResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("credits_balance") val creditsBalance: Int,
    @SerializedName("subscription_level") val subscriptionLevel: Int
)

data class MigrationWaitingResponse(
    @SerializedName("waiting") val waiting: Boolean,
    @SerializedName("message") val message: String?
)

data class FeatureConfig(
    @SerializedName("features") val features: Map<String, Boolean>,
    @SerializedName("configs") val configs: Map<String, String>
)

data class GlobalErrorConfig(
    @SerializedName("errors") val errors: Map<Int, String>
)