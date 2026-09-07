// Root build file. Plugin versions are declared here (with apply false) and applied per-module
// in core/build.gradle.kts and wearos-app/build.gradle.kts.
plugins {
    // Versions resolved 2026-09-07 against the real Google/Maven repos and verified to build
    // on this machine (Gradle 9.3.0, JDK 25, Android SDK 36). AGP 8.13.2 is the newest 8.x and
    // the one that tolerates Gradle 9.x + compileSdk 36; Kotlin 2.1.10 already builds :core
    // clean on Gradle 9.3. See PLAN.md Phase 0c.
    id("com.android.application") version "8.13.2" apply false
    kotlin("android") version "2.1.10" apply false
    kotlin("jvm") version "2.1.10" apply false
    // Kotlin 2.0+ split the Compose compiler out into its own Gradle plugin — required
    // whenever `buildFeatures.compose = true` is set anywhere (wearos-app).
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
}
