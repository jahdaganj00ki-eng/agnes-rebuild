package com.sobrr.agnes.feature_auth.model

import com.sobrr.agnes.data.model.base.BaseNoResponse
import com.sobrr.agnes.data.model.base.BaseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {

    @POST("api/auth/token_by_email")
    suspend fun loginByEmail(@Body request: AuthEmailRequest): BaseResponse<TokenResponse>

    @POST("api/v1/user/login")
    suspend fun loginEmailPassword(@Body request: AuthEmailRequest): BaseResponse<TokenResponse>

    @POST("api/v1/user/login")
    suspend fun loginPhonePassword(@Body request: AuthPhoneRequest): BaseResponse<TokenResponse>

    @POST("api/v1/user/register")
    suspend fun registerByEmail(@Body request: RegisterByEmailRequest): BaseResponse<TokenResponse>

    @POST("api/v1/user/refresh-token")
    suspend fun refreshToken(): BaseResponse<TokenResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): BaseResponse<AuthMeUserDto>

    @GET("api/v2/user/profile")
    suspend fun getProfileV2(): BaseResponse<OwnerProfileResponse>

    @PATCH("api/v1/user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): BaseResponse<OwnerProfileResponse>

    @POST("api/auth/update_user_avatar")
    suspend fun updateAvatar(@Body request: ChangeAvatarRequest): BaseResponse<OwnerProfileResponse>

    @POST("api/auth/update_user_name")
    suspend fun updateUserName(@Body request: ChangeUserNameRequest): BaseResponse<OwnerProfileResponse>

    @POST("api/v1/user/code/send")
    suspend fun sendCode(@Body request: SendCodeRequest): BaseResponse<Unit>

    @POST("api/v1/user/code/verify")
    suspend fun verifyCode(@Body request: VerifyCodeRequest): BaseResponse<Unit>

    @POST("api/v1/user/bind_email")
    suspend fun bindEmail(@Body request: BindEmailRequest): BaseResponse<Unit>

    @POST("api/v1/user/bind_phone")
    suspend fun bindPhone(@Body request: BindPhoneRequest): BaseResponse<Unit>

    @POST("api/v1/user/reset_password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): BaseResponse<Unit>

    @POST("api/v1/user/timezone")
    suspend fun updateTimezone(@Body request: TimezoneRequest): BaseResponse<Unit>

    @DELETE("api/v1/user/account")
    suspend fun deleteAccount(): BaseNoResponse

    @POST("api/v1/fcm/token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): BaseResponse<Unit>

    @GET("api/auth/clear_firebase_token")
    suspend fun clearFirebaseToken(): BaseNoResponse

    @GET("api/v1/user/migration/waiting")
    suspend fun getMigrationWaiting(): BaseResponse<MigrationWaitingResponse>

    @GET("api/v1/version/check")
    suspend fun checkVersion(): BaseResponse<VersionCheckResponse>

    @POST("api/v1/security/parameter")
    suspend fun getSecurityParameter(): BaseResponse<SecurityParameterResponse>
}

data class FcmTokenRequest(
    @SerializedName("token") val token: String,
    @SerializedName("platform") val platform: String = "android"
)

data class VersionCheckResponse(
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("force_update") val forceUpdate: Boolean,
    @SerializedName("update_url") val updateUrl: String?,
    @SerializedName("changelog") val changelog: String?
)

data class SecurityParameterResponse(
    @SerializedName("parameter") val parameter: String,
    @SerializedName("expires_at") val expiresAt: Long
)