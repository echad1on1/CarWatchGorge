// Wear OS application module. Depends on :core for every domain model and manager; contains
// ONLY presentation (Compose screens) and, eventually, the real hardware implementations
// (NfcProvider, BluetoothProvider, etc.) — see hardware/README.md in this module once those
// exist.
//
// Versions resolved 2026-09-07 against the real Google Maven / Maven Central repos and
// verified to assemble on this machine (Gradle 9.3.0, JDK 25, Android SDK 36). See PLAN.md
// Phase 0c for the resolution notes. Wear Compose stable tops out at 1.6.2 (1.7.x is still
// alpha), so those coordinates were already current.
plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dashboard.wearos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dashboard.wearos"
        minSdk = 30 // Wear OS 3+
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    buildFeatures {
        compose = true
        buildConfig = true // needed for BuildConfig.DEBUG, used to gate the dev-controls entry point
    }

    buildTypes {
        // The ⚙ dev-controls entry point is gated on BuildConfig.DEBUG, so a release build
        // hides it automatically — that is the spec's "these controls will later be removed
        // or hidden". Minify is off for now (core uses no reflection; enabling R8 is a
        // separate, low-risk follow-up — see PLAN.md Phase 5).
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":core"))

    // Core Compose (shared with mobile Compose; Wear OS uses these as-is per Google's guidance).
    // The BOM governs every androidx.compose.* version — do not pin them individually.
    // BOM 2025.10.01 → Compose 1.9.4 (compileSdk 36, AGP 8.9+). Newer BOMs pull Compose
    // 1.12.x which demands AGP 9.1 + compileSdk 37 (not installed here).
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Compose for Wear OS — Wear-specific material/foundation, NOT the mobile material3 library
    // (mixing the two is explicitly discouraged by Google's own Wear OS Compose guidance).
    // 1.6.2 is the latest STABLE Wear Compose (1.7.x is alpha as of 2026-09).
    implementation("androidx.wear.compose:compose-material3:1.6.2")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.wear.compose:compose-ui-tooling:1.6.2")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")

    // Phone ↔ watch transport for navigation checkpoints (see WearDataLayerBluetoothProvider).
    implementation("com.google.android.gms:play-services-wearable:19.0.0")

    // Per-viewer settings persistence behind core's SettingsStore (Phase 5).
    implementation("androidx.datastore:datastore-preferences:1.1.7")
}
