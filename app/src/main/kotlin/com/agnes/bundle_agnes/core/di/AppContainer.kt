package com.agnes.bundle_agnes.core.di

import android.content.Context
import com.agnes.bundle_agnes.BuildConfig
import com.agnes.bundle_agnes.core.di.qualifiers.Mock
import com.agnes.bundle_agnes.core.di.qualifiers.Live
import com.agnes.bundle_agnes.feature.auth.repository.AuthRepository
import com.agnes.bundle_agnes.feature.auth.repository.MockAuthRepository
import com.agnes.bundle_agnes.feature.auth.repository.NetworkAuthRepository
import com.agnes.bundle_agnes.feature.chat.repository.ChatRepository
import com.agnes.bundle_agnes.feature.chat.repository.MockChatRepository
import com.agnes.bundle_agnes.feature.chat.repository.NetworkChatRepository
import com.sobrr.agnes.data.network.AuthInterceptor
import com.sobrr.agnes.data.network.RetrofitModule
import com.sobrr.agnes.data.network.provideGson
import com.sobrr.agnes.data.network.provideLoggingInterceptor
import com.sobrr.agnes.data.network.provideOkHttpClient
import com.sobrr.agnes.data.network.provideRetrofit
import com.sobrr.agnes.feature_auth.model.AuthApi
import com.sobrr.agnes.feature_chat.model.ChatApi
import okhttp3.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class AppContainer private constructor(private val context: Context) {

    // Configuration
    val apiProfile: String = BuildConfig.API_PROFILE
    val baseUrl: String = BuildConfig.BASE_URL
    val h5Url: String = BuildConfig.H5_URL
    val enableFirebase: Boolean = BuildConfig.ENABLE_FIREBASE
    val enableTencentIm: Boolean = BuildConfig.ENABLE_TENCENT_IM
    val enableAttribution: Boolean = BuildConfig.ENABLE_ATTRIBUTION
    val enableBilling: Boolean = BuildConfig.ENABLE_BILLING
    val enablePush: Boolean = BuildConfig.ENABLE_PUSH

    // Core network components
    private val gson = provideGson()
    private val loggingInterceptor = provideLoggingInterceptor()
    private val authInterceptor = AuthInterceptor { tokenProvider() }
    private val okHttpClient = RetrofitModule.provideOkHttpClient(context, authInterceptor, loggingInterceptor)
    private val retrofit = RetrofitModule.provideRetrofit(baseUrl, okHttpClient, gson)

    // API Services
    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val chatApi: ChatApi = retrofit.create(ChatApi::class.java)

    // Token provider (to be implemented with KeystoreDataStore)
    private var _accessToken: String? = null
    private var _refreshToken: String? = null

    fun tokenProvider(): String? = _accessToken

    fun setTokens(accessToken: String?, refreshToken: String?) {
        _accessToken = accessToken
        _refreshToken = refreshToken
        // TODO: Persist to KeystoreDataStore
    }

    fun clearTokens() {
        _accessToken = null
        _refreshToken = null
        // TODO: Clear from KeystoreDataStore
    }

    // Repositories - bound by API_PROFILE
    val authRepository: AuthRepository by lazy {
        if (apiProfile == "MOCK") {
            MockAuthRepository()
        } else {
            NetworkAuthRepository(authApi)
        }
    }

    val chatRepository: ChatRepository by lazy {
        if (apiProfile == "MOCK") {
            MockChatRepository()
        } else {
            NetworkChatRepository(chatApi)
        }
    }

    // Qualifiers for MOCK/LIVE
    @Mock
    val mockAuthRepository: AuthRepository = MockAuthRepository()

    @Live
    val liveAuthRepository: AuthRepository = NetworkAuthRepository(authApi)

    @Mock
    val mockChatRepository: ChatRepository = MockChatRepository()

    @Live
    val liveChatRepository: ChatRepository = NetworkChatRepository(chatApi)
}

// Qualifier annotations for MOCK/LIVE
package com.agnes.bundle_agnes.core.di.qualifiers

import jakarta.inject.Qualifier
import kotlin.annotation.Retention
import kotlin.annotation.Target
import java.lang.annotation.ElementType.ANNOTATION_CLASS
import java.lang.annotation.ElementType.FIELD
import java.lang.annotation.ElementType.PARAMETER
import java.lang.annotation.RetentionPolicy.RUNTIME

@Qualifier
@Retention(RUNTIME)
@Target(ANNOTATION_CLASS, FIELD, PARAMETER)
annotation class Mock

@Qualifier
@Retention(RUNTIME)
@Target(ANNOTATION_CLASS, FIELD, PARAMETER)
annotation class Live