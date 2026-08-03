package com.agnes.bundle_agnes.feature.auth.repository

import com.agnes.bundle_agnes.core.di.AppContainer
import com.sobrr.agnes.data.model.base.BaseResponse
import com.sobrr.agnes.data.network.ApiResult
import com.sobrr.agnes.feature_auth.model.AuthApi
import com.sobrr.agnes.feature_auth.model.AuthEmailRequest
import com.sobrr.agnes.feature_auth.model.AuthMeUserDto
import com.sobrr.agnes.feature_auth.model.AuthPhoneRequest
import com.sobrr.agnes.feature_auth.model.BindEmailRequest
import com.sobrr.agnes.feature_auth.model.BindPhoneRequest
import com.sobrr.agnes.feature_auth.model.ChangeAvatarRequest
import com.sobrr.agnes.feature_auth.model.ChangeUserNameRequest
import com.sobrr.agnes.feature_auth.model.FcmTokenRequest
import com.sobrr.agnes.feature_auth.model.RegisterByEmailRequest
import com.sobrr.agnes.feature_auth.model.ResetPasswordRequest
import com.sobrr.agnes.feature_auth.model.SendCodeRequest
import com.sobrr.agnes.feature_auth.model.TimezoneRequest
import com.sobrr.agnes.feature_auth.model.TokenResponse
import com.sobrr.agnes.feature_auth.model.UpdateProfileRequest
import com.sobrr.agnes.feature_auth.model.VerifyCodeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkAuthRepository(
    private val authApi: AuthApi,
    private val appContainer: AppContainer
) : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthMeUserDto?>(null)
    override val currentUser: StateFlow<AuthMeUserDto?> = _currentUser

    override val isLoggedIn: Boolean
        get() = _currentUser.value != null

    override val accessToken: String?
        get() = appContainer.tokenProvider()

    private fun <T> handleApiResult(result: ApiResult<BaseResponse<T>>): Result<T> {
        return when (result) {
            is ApiResult.Success -> {
                if (result.data.isSuccess()) {
                    result.data.data?.let { Result.success(it) } ?: Result.failure(IllegalStateException("Empty data"))
                } else {
                    Result.failure(ApiException(result.data.code, result.data.message))
                }
            }
            is ApiResult.Error -> Result.failure(ApiException(result.code, result.message))
        }
    }

    override suspend fun loginByEmail(email: String, password: String): Result<TokenResponse> {
        val result = authApi.loginEmailPassword(AuthEmailRequest(email, password))
        return handleApiResult(result)
    }

    override suspend fun loginByPhone(phone: String, code: String): Result<TokenResponse> {
        val result = authApi.loginPhonePassword(AuthPhoneRequest(phone, code))
        return handleApiResult(result)
    }

    override suspend fun loginByGoogle(idToken: String, accessToken: String?): Result<TokenResponse> {
        // Not directly supported in AuthApi, would need a different endpoint
        return Result.failure(UnsupportedOperationException("Google login not implemented in NetworkAuthRepository"))
    }

    override suspend fun registerByEmail(email: String, password: String, username: String?, invitationCode: String?): Result<TokenResponse> {
        val result = authApi.registerByEmail(RegisterByEmailRequest(email, password, username, invitationCode))
        return handleApiResult(result)
    }

    override suspend fun refreshToken(): Result<TokenResponse> {
        val result = authApi.refreshToken()
        return handleApiResult(result)
    }

    override suspend fun logout(): Result<Unit> {
        val result = authApi.clearFirebaseToken()
        if (result is ApiResult.Success) {
            _currentUser.value = null
            appContainer.clearTokens()
            return Result.success(Unit)
        }
        return Result.failure(ApiException(-1, "Logout failed"))
    }

    override suspend fun sendCode(contact: String, type: String): Result<Unit> {
        val result = authApi.sendCode(SendCodeRequest(contact, type))
        return handleApiResult(result)
    }

    override suspend fun verifyCode(contact: String, code: String, type: String): Result<Unit> {
        val result = authApi.verifyCode(VerifyCodeRequest(contact, code, type))
        return handleApiResult(result)
    }

    override suspend fun bindEmail(email: String, code: String): Result<Unit> {
        val result = authApi.bindEmail(BindEmailRequest(email, code))
        return handleApiResult(result)
    }

    override suspend fun bindPhone(phone: String, code: String): Result<Unit> {
        val result = authApi.bindPhone(BindPhoneRequest(phone, code))
        return handleApiResult(result)
    }

    override suspend fun resetPassword(email: String, newPassword: String, code: String): Result<Unit> {
        val result = authApi.resetPassword(ResetPasswordRequest(email, newPassword, code))
        return handleApiResult(result)
    }

    override suspend fun updateProfile(username: String?, avatar: String?, bio: String?): Result<AuthMeUserDto> {
        val result = authApi.updateProfile(UpdateProfileRequest(username, avatar, bio))
        return handleApiResult(result).also { if (it.isSuccess) _currentUser.value = it.getOrNull() }
    }

    override suspend fun updateAvatar(avatarUrl: String): Result<AuthMeUserDto> {
        val result = authApi.updateAvatar(ChangeAvatarRequest(avatarUrl))
        return handleApiResult(result).also { if (it.isSuccess) _currentUser.value = it.getOrNull() }
    }

    override suspend fun updateUserName(username: String): Result<AuthMeUserDto> {
        val result = authApi.updateUserName(ChangeUserNameRequest(username))
        return handleApiResult(result).also { if (it.isSuccess) _currentUser.value = it.getOrNull() }
    }

    override suspend fun updateTimezone(timezone: String): Result<Unit> {
        val result = authApi.updateTimezone(TimezoneRequest(timezone))
        return handleApiResult(result)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val result = authApi.deleteAccount()
        if (result is ApiResult.Success && result.data.isSuccess()) {
            _currentUser.value = null
            appContainer.clearTokens()
            return Result.success(Unit)
        }
        return Result.failure(ApiException(-1, "Delete account failed"))
    }

    override suspend fun registerFcmToken(token: String): Result<Unit> {
        val result = authApi.registerFcmToken(FcmTokenRequest(token))
        return handleApiResult(result)
    }

    override suspend fun clearFirebaseToken(): Result<Unit> {
        val result = authApi.clearFirebaseToken()
        return handleApiResult(result)
    }
}

class ApiException(val code: Int, val message: String) : Exception(message)