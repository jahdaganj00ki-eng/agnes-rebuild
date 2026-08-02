plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
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
        // Environments (README §2): dev / test / preview / prod → BASE_URL + H5_URL build fields
    }
    // Dependencies (see docs/03-release-checklist.md):
    // retrofit2 + okhttp3 + gson, androidx.room3, datastore, lifecycle/credentials,
    // compose BOM, media3, play-billing-ktx, play-core, firebase-bom (analytics, messaging),
    // tencent IM + TIMPush, adjust sdk, appsflyer, startup/profileinstaller/emoji2/window.
    buildFeatures { compose = true }
}
