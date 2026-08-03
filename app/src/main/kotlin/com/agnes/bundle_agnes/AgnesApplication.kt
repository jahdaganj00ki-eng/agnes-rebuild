package com.agnes.bundle_agnes

import android.app.Application
import android.content.Context
import com.agnes.bundle_agnes.core.di.AppContainer
import com.sobrr.agnes.data.network.RetrofitModule
import com.sobrr.agnes.data.network.provideGson
import com.sobrr.agnes.data.network.provideLoggingInterceptor
import com.sobrr.agnes.data.network.provideOkHttpClient
import com.sobrr.agnes.data.network.provideRetrofit
import okhttp3.HttpLoggingInterceptor

class AgnesApplication : Application() {

    private lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()

        // Initialize AppContainer with DI
        appContainer = AppContainer(this)

        // Initialize any global configs
        initializeThirdParty()
    }

    private fun initializeThirdParty() {
        // Firebase, Tencent IM, etc. initialization would go here
        // Gated by BuildConfig.ENABLE_FIREBASE, etc.
    }

    fun getAppContainer(): AppContainer = appContainer

    companion object {
        fun getAppContainer(context: Context): AppContainer {
            return (context.applicationContext as AgnesApplication).getAppContainer()
        }
    }
}