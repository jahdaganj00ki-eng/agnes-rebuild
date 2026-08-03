import com.android.build.api.dsl.BuildType

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
}

android {
    namespace = "com.agnes.bundle_agnes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sobrr.agnes"
        minSdk = 26
        targetSdk = 34
        versionCode = 3000061
        versionName = "3.0.61"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "BASE_URL", "\"https://dev.api.example.com\"")
        buildConfigField("String", "H5_URL", "\"https://dev.h5.example.com\"")
        buildConfigField("String", "API_PROFILE", "\"MOCK\"")
        buildConfigField("boolean", "ENABLE_FIREBASE", "false")
        buildConfigField("boolean", "ENABLE_TENCENT_IM", "false")
        buildConfigField("boolean", "ENABLE_ATTRIBUTION", "false")
        buildConfigField("boolean", "ENABLE_BILLING", "false")
        buildConfigField("boolean", "ENABLE_PUSH", "false")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,NOTICE}*"
        }
    }

    flavorDimensions "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"https://dev.api.example.com\"")
            buildConfigField("String", "H5_URL", "\"https://dev.h5.example.com\"")
            buildConfigField("String", "API_PROFILE", "\"MOCK\"")
            buildConfigField("boolean", "ENABLE_FIREBASE", "false")
            buildConfigField("boolean", "ENABLE_TENCENT_IM", "false")
            buildConfigField("boolean", "ENABLE_ATTRIBUTION", "false")
            buildConfigField("boolean", "ENABLE_BILLING", "false")
            buildConfigField("boolean", "ENABLE_PUSH", "false")
        }
        create("test") {
            dimension = "environment"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("String", "H5_URL", "\"http://10.0.2.2:8080/h5\"")
            buildConfigField("String", "API_PROFILE", "\"LIVE\"")
            buildConfigField("boolean", "ENABLE_FIREBASE", "false")
            buildConfigField("boolean", "ENABLE_TENCENT_IM", "false")
            buildConfigField("boolean", "ENABLE_ATTRIBUTION", "false")
            buildConfigField("boolean", "ENABLE_BILLING", "false")
            buildConfigField("boolean", "ENABLE_PUSH", "false")
        }
        create("preview") {
            dimension = "environment"
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            buildConfigField("String", "BASE_URL", "\"https://preview.api.example.com\"")
            buildConfigField("String", "H5_URL", "\"https://preview.h5.example.com\"")
            buildConfigField("String", "API_PROFILE", "\"LIVE\"")
            buildConfigField("boolean", "ENABLE_FIREBASE", "true")
            buildConfigField("boolean", "ENABLE_TENCENT_IM", "true")
            buildConfigField("boolean", "ENABLE_ATTRIBUTION", "true")
            buildConfigField("boolean", "ENABLE_BILLING", "true")
            buildConfigField("boolean", "ENABLE_PUSH", "true")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.example.com\"")
            buildConfigField("String", "H5_URL", "\"https://h5.example.com\"")
            buildConfigField("String", "API_PROFILE", "\"LIVE\"")
            buildConfigField("boolean", "ENABLE_FIREBASE", "true")
            buildConfigField("boolean", "ENABLE_TENCENT_IM", "true")
            buildConfigField("boolean", "ENABLE_ATTRIBUTION", "true")
            buildConfigField("boolean", "ENABLE_BILLING", "true")
            buildConfigField("boolean", "ENABLE_PUSH", "true")
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    signingConfigs {
        create("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn", "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    // Compose
    val composeBomVersion: String by project
    val kotlinxCoroutinesVersion: String by project
    val retrofitVersion: String by project
    val okhttpVersion: String by project
    val gsonVersion: String by project
    val coilVersion: String by project
    val roomVersion: String by project
    val datastoreVersion: String by project
    val lifecycleVersion: String by project
    val navigationVersion: String by project
    val media3Version: String by project
    val playBillingVersion: String by project
    val firebaseBomVersion: String by project
    val junitVersion: String by project
    val junitJupiterVersion: String by project
    val turbineVersion: String by project
    val mockwebserverVersion: String by project
    val espressoVersion: String by project
    val koinVersion: String by project

    // Compose
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.lifecycle:lifecycle-runtime-compose")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$navigationVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")

    // Retrofit / OkHttp / Gson
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("com.squareup.okhttp3:okhttp-sse:$okhttpVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")

    // Coil
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("io.coil-kt:coil-base:$coilVersion")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    // ksp("androidx.room:room-compiler:$roomVersion") // Requires KSP plugin

    // DataStore
    implementation("androidx.datastore:datastore-preferences:$datastoreVersion")
    implementation("androidx.datastore:datastore-core:$datastoreVersion")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // Media3
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")

    // Play Billing
    implementation("com.android.billingclient:billing-ktx:$playBillingVersion")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:$firebaseBomVersion"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Koin (lightweight DI)
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-android:$koinVersion")
    implementation("io.insert-koin:koin-androidx-compose:$koinVersion")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:$mockwebserverVersion")
    testImplementation("com.google.truth:truth:1.1.5")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.0")
    androidTestImplementation("androidx.test:rules:1.6.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:$espressoVersion")
}