plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.mb.tvlauncher"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.mb.appletvlauncher"
        minSdk = 21
        targetSdk = 22
        versionCode = 5
        versionName = "1.0.4"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
