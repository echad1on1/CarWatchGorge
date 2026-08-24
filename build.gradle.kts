// Root build file. Plugin versions are declared here (with apply false) and applied per-module
// in core/build.gradle.kts and wearos-app/build.gradle.kts.
//
// NOTE: these plugin/AGP/Kotlin versions could not be verified by actually running Gradle in
// the sandbox this project was built in (no network access to services.gradle.org or Google's
// Maven repo — see README). They're reasonable as of this writing but should be bumped to
// whatever Android Studio's project wizard suggests when this is first opened there.
plugins {
    id("com.android.application") version "8.9.1" apply false
    kotlin("android") version "2.1.10" apply false
    kotlin("jvm") version "2.1.10" apply false
    // Kotlin 2.0+ split the Compose compiler out into its own Gradle plugin — required
    // whenever `buildFeatures.compose = true` is set anywhere (wearos-app).
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
}
