// Root build file for the Agnes-style rebuild scaffold.
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.2")
    }
}

// Shared versions - defined in a way accessible to subprojects
subprojects {
    afterEvaluate {
        if (hasProperty("android")) {
            // Android project versions (Java 11 compatible)
            ext["composeBomVersion"] = "2024.02.00"
            ext["kotlinxCoroutinesVersion"] = "1.8.1"
            ext["kotlinxSerializationVersion"] = "1.6.3"
            ext["retrofitVersion"] = "2.11.0"
            ext["okhttpVersion"] = "4.12.0"
            ext["gsonVersion"] = "2.11.0"
            ext["coilVersion"] = "2.6.0"
            ext["roomVersion"] = "2.6.1"
            ext["datastoreVersion"] = "1.1.7"
            ext["lifecycleVersion"] = "2.7.0"
            ext["navigationVersion"] = "2.7.6"
            ext["media3Version"] = "1.4.1"
            ext["playBillingVersion"] = "7.0.0"
            ext["firebaseBomVersion"] = "33.0.0"
            ext["junitVersion"] = "4.13.2"
            ext["junitJupiterVersion"] = "5.10.2"
            ext["turbineVersion"] = "1.0.0"
            ext["mockwebserverVersion"] = "4.11.0"
            ext["espressoVersion"] = "3.5.1"
            ext["koinVersion"] = "3.5.3"
        } else if (name == "sidecar") {
            // Sidecar project versions
            ext["ktorVersion"] = "2.3.12"
            ext["logbackVersion"] = "1.5.6"
            ext["slf4jVersion"] = "2.0.9"
            ext["kotlinxCoroutinesVersion"] = "1.8.1"
            ext["kotlinxSerializationVersion"] = "1.6.3"
        }
    }
}