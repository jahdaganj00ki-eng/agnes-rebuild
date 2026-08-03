# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep data classes for Gson serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Room entities
-keep class * extends androidx.room.Entity { *; }
-keep class * extends androidx.room.Database { *; }

# Keep Koin modules
-keep class * implements org.koin.core.module.Module { *; }

# Keep BuildConfig
-keep class com.agnes.bundle_agnes.BuildConfig { *; }

# Keep DataStore preferences
-keep class androidx.datastore.preferences.core.Preferences { *; }

# Keep coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Ktor client serialization
-keep class io.ktor.serialization.** { *; }