package com.agnes.bundle_agnes.feature.auth.repository

import com.sobrr.agnes.feature_auth.model.AuthMeUserDto
import com.sobrr.agnes.feature_auth.model.TokenResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicReference

class MockAuthRepository : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthMeUserDto?>(null)
    override val currentUser: StateFlow<AuthMeUserDto?> = _currentUser

    override val isLoggedIn: Boolean
        get() = _currentUser.value != null

    override val accessToken: String?
        get() = if (isLoggedIn) "mock_access_token_${System.currentTimeMillis()}" else null

    override suspend fun loginByEmail(email: String, password: String): Result<TokenResponse> {
        delay(500) // Simulate network
        val user = AuthMeUserDto(
            id = 1,
            username = email.split("@")[0],
            email = email,
            phone = null,
            avatar = null,
            timezone = "UTC",
            createdAt = "2024-01-01T00:00:00Z"
        )
        _currentUser.value = user
        return Result.success(TokenResponse(
            accessToken = "mock_access_token",
            refreshToken = "mock_refresh_token",
            expiresIn = 3600
        ))
    }

    override suspend fun loginByPhone(phone: String, code: String): Result<TokenResponse> {
        delay(500)
        val user = AuthMeUserDto(
            id = 2,
            username = "user_$phone",
            email = null,
            phone = phone,
            avatar = null,
            timezone = "UTC",
            createdAt = "2024-01-01T00:00:00Z"
        )
        _currentUser.value = user
        return Result.success(TokenResponse(
            accessToken = "mock_access_token",
            refreshToken = "mock_refresh_token",
            expiresIn = 3600
        ))
    }

    override suspend fun loginByGoogle(idToken: String, accessToken: String?): Result<TokenResponse> {
        delay(500)
        val user = AuthMeUserDto(
            id = 3,
            username = "google_user",
            email = "user@gmail.com",
            phone = null,
            avatar = "https://example.com/avatar.jpg",
            timezone = "UTC",
            createdAt = "2024-01-01T00:00:00Z"
        )
        _currentUser.value = user
        return Result.success(TokenResponse(
            accessToken = "mock_access_token",
            refreshToken = "mock_refresh_token",
            expiresIn = 3600
        ))
    }

    override suspend fun registerByEmail(email: String, password: String, username: String?, invitationCode: String?): Result<TokenResponse> {
        delay(500)
        val user = AuthMeUserDto(
            id = 4,
            username = username ?: email.split("@")[0],
            email = email,
            phone = null,
            avatar = null,
            timezone = "UTC",
            createdAt = "2024-01-01T00:00:00Z"
        )
        _currentUser.value = user
        return Result.success(TokenResponse(
            accessToken = "mock_access_token",
            refreshToken = "mock_refresh_token",
            expiresIn = 3600
        ))
    }

    override suspend fun refreshToken(): Result<TokenResponse> {
        delay(200)
        return Result.success(TokenResponse(
            accessToken = "mock_refreshed_access_token",
            refreshToken = "mock_refreshed_refresh_token",
            expiresIn = 3600
        ))
    }

    override suspend fun logout(): Result<Unit> {
        delay(100)
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun sendCode(contact: String, type: String): Result<Unit> {
        delay(300)
        // Mock: code is always "123456"
        return Result.success(Unit)
    }

    override suspend fun verifyCode(contact: String, code: String, type: String): Result<Unit> {
        delay(300)
        return if (code == "123456") Result.success(Unit) else Result.failure(IllegalArgumentException("Invalid code"))
    }

    override suspend fun bindEmail(email: String, code: String): Result<Unit> {
        delay(300)
        return if (code == "123456") Result.success(Unit) else Result.failure(IllegalArgumentException("Invalid code"))
    }

    override suspend fun bindPhone(phone: String, code: String): Result<Unit> {
        delay(300)
        return if (code == "123456") Result.success(Unit) else Result.failure(IllegalArgumentException("Invalid code"))
    }

    override suspend fun resetPassword(email: String, newPassword: String, code: String): Result<Unit> {
        delay(300)
        return if (code == "123456") Result.success(Unit) else Result.failure(IllegalArgumentException("Invalid code"))
    }

    override suspend fun updateProfile(username: String?, avatar: String?, bio: String?): Result<AuthMeUserDto> {
        delay(300)
        val current = _currentUser.value ?: return Result.failure(IllegalStateException("Not logged in"))
        val updated = current.copy(username = username ?: current.username)
        _currentUser.value = updated
        return Result.success(updated)
    }

    override suspend fun updateAvatar(avatarUrl: String): Result<AuthMeUserDto> {
        delay(300)
        val current = _currentUser.value ?: return Result.failure(IllegalStateException("Not logged in"))
        val updated = current.copy(avatar = avatarUrl)
        _currentUser.value = updated
        return Result.success(updated)
    }

    override suspend fun updateUserName(username: String): Result<AuthMeUserDto> {
        delay(300)
        val current = _currentUser.value ?: return Result.failure(IllegalStateException("Not logged in"))
        val updated = current.copy(username = username)
        _currentUser.value = updated
        return Result.success(updated)
    }

    override suspend fun updateTimezone(timezone: String): Result<Unit> {
        delay(100)
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        delay(500)
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun registerFcmToken(token: String): Result<Unit> {
        delay(100)
        return Result.success(Unit)
    }

    override suspend fun clearFirebaseToken(): Result<Unit> {
        delay(100)
        return Result.success(Unit)
    }
}