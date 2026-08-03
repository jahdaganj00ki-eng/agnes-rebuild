package com.agnes.bundle_agnes.feature.auth.repository

import com.sobrr.agnes.feature_auth.model.AuthMeUserDto
import com.sobrr.agnes.feature_auth.model.TokenResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<AuthMeUserDto?>
    val isLoggedIn: Boolean
    val accessToken: String?

    suspend fun loginByEmail(email: String, password: String): Result<TokenResponse>
    suspend fun loginByPhone(phone: String, code: String): Result<TokenResponse>
    suspend fun loginByGoogle(idToken: String, accessToken: String?): Result<TokenResponse>
    suspend fun registerByEmail(email: String, password: String, username: String?, invitationCode: String?): Result<TokenResponse>
    suspend fun refreshToken(): Result<TokenResponse>
    suspend fun logout(): Result<Unit>
    suspend fun sendCode(contact: String, type: String): Result<Unit>
    suspend fun verifyCode(contact: String, code: String, type: String): Result<Unit>
    suspend fun bindEmail(email: String, code: String): Result<Unit>
    suspend fun bindPhone(phone: String, code: String): Result<Unit>
    suspend fun resetPassword(email: String, newPassword: String, code: String): Result<Unit>
    suspend fun updateProfile(username: String?, avatar: String?, bio: String?): Result<AuthMeUserDto>
    suspend fun updateAvatar(avatarUrl: String): Result<AuthMeUserDto>
    suspend fun updateUserName(username: String): Result<AuthMeUserDto>
    suspend fun updateTimezone(timezone: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun registerFcmToken(token: String): Result<Unit>
    suspend fun clearFirebaseToken(): Result<Unit>
}