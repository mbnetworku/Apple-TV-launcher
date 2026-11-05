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
        versionCode = 23
        versionName = "1.2.2"
    }
    // Release = not debuggable, so ART fully compiles it (much smoother on slow TV CPUs).
    // Signed with the same debug key as before, so it installs over the current build and keeps its settings.
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
    // targetSdk 22 is deliberate (see CLAUDE.md); don't let the Play Store target-SDK lint check block release builds.
    lint { checkReleaseBuilds = false }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
