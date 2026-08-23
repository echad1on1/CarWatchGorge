// Wear OS application module. Depends on :core for every domain model and manager; contains
// ONLY presentation (Compose screens) and, eventually, the real hardware implementations
// (NfcProvider, BluetoothProvider, etc.) — see hardware/README.md in this module once those
// exist.
//
// NOTE: version numbers below were sourced from https://developer.android.com/training/wearables/compose
// at the time this was written, but could not be verified by actually running Gradle in the
// sandbox this project was built in (no network access to Google's Maven repo — see root
// README). Treat them as a reasonable starting point, not gospel — Android Studio will flag
// anything outdated when this is first opened and synced there.
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.dashboard.wearos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dashboard.wearos"
        minSdk = 30 // Wear OS 3+
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    buildFeatures {
        compose = true
        buildConfig = true // needed for BuildConfig.DEBUG, used to gate the dev-controls entry point
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))

    // Core Compose (shared with mobile Compose; Wear OS uses these as-is per Google's guidance).
    val composeBom = platform("androidx.compose:compose-bom:2026.05.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.3")

    // Compose for Wear OS — Wear-specific material/foundation, NOT the mobile material3 library
    // (mixing the two is explicitly discouraged by Google's own Wear OS Compose guidance).
    implementation("androidx.wear.compose:compose-material3:1.6.2")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.wear.compose:compose-ui-tooling:1.6.2")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
}
