// The phone-side companion app. Runs on the user's phone, reads on-screen turn-by-turn text
// from whatever navigation app is running (via AccessibilityService — see
// NavigationAccessibilityService.kt and docs/android-integration-research.md for why this
// approach was chosen over the alternatives), and sends the resulting checkpoints to the watch
// over BluetoothPhoneCommunication (from :core).
//
// STATUS: written, not yet build-verified or run on a real phone — see this module's README.md.
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.dashboard.phoneapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dashboard.phoneapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.gms:play-services-wearable:20.0.0")
}
