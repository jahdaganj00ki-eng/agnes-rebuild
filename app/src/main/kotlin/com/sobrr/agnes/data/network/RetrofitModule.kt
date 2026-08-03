package com.sobrr.agnes.data.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sobrr.agnes.data.model.base.BaseNoResponse
import com.sobrr.agnes.data.model.base.BaseResponse
import com.sobrr.agnes.data.network.fileupload.FileUploadApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitModule {

    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 60L
    private const val WRITE_TIMEOUT = 60L

    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()
    }

    fun provideOkHttpClient(
        context: Context,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(RefreshTokenInterceptor(context, authInterceptor))
            .build()
    }

    fun provideRetrofit(
        baseUrl: String,
        client: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(ApiResultCallAdapterFactory())
            .build()
    }

    fun provideFileUploadApi(retrofit: Retrofit): FileUploadApi {
        return retrofit.create(FileUploadApi::class.java)
    }
}

// Auth Interceptor for adding Bearer token
class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider()
        return if (token != null && token.isNotEmpty()) {
            val authorizedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authorizedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}

// Refresh Token Interceptor for handling 401
class RefreshTokenInterceptor(
    private val context: Context,
    private val authInterceptor: AuthInterceptor
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            // Try to refresh token
            val refreshed = tryRefreshToken()
            if (refreshed) {
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer ${getAccessToken()}")
                    .build()
                return chain.proceed(newRequest)
            }
        }
        return response
    }

    private fun tryRefreshToken(): Boolean {
        // TODO: Implement actual token refresh logic
        return false
    }

    private fun getAccessToken(): String? {
        // TODO: Get from token store
        return null
    }
}

// Logging Interceptor
fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}