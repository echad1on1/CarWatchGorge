// Root build file. Plugin versions are declared here (with apply false) and applied per-module
// in core/build.gradle.kts and wearos-app/build.gradle.kts.
//
// NOTE: these plugin/AGP/Kotlin versions could not be verified by actually running Gradle in
// the sandbox this project was built in (no network access to services.gradle.org or Google's
// Maven repo — see README). They're reasonable as of this writing but should be bumped to
// whatever Android Studio's project wizard suggests when this is first opened there.
plugins {
    id("com.android.application") version "8.7.0" apply false
    kotlin("android") version "2.0.21" apply false
    kotlin("jvm") version "2.0.21" apply false
}
