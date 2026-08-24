// `core` is deliberately a plain Kotlin/JVM module, not an Android library module — it has zero
// Android dependency (see README's "Target platform decision" section), which is what makes it
// portable to whatever the final UI toolkit turns out to be. wearos-app depends on this module
// like any other Kotlin library.
plugins {
    kotlin("jvm")
}

// Pins the compiled bytecode target to Java 17 (matching wearos-app), WITHOUT requiring Gradle
// to locate or download an actual JDK 17 installation — that's what jvmToolchain(...) does, and
// it failed on a machine that only had JDK 25 installed with no toolchain download configured.
// This approach just passes --release 17-equivalent flags to whatever JDK is already running
// Gradle (any modern JDK can cross-compile down to an older bytecode target).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

// Repositories are declared once, project-wide, in settings.gradle.kts — Gradle doesn't allow
// declaring them again per-module.

// No dependencies today. If JUnit5 becomes available once this is opened in an environment with
// normal Maven access, add it here as a testImplementation and migrate
// src/test/kotlin/.../testing/TestHarness.kt's callers over (see its class doc for why it
// exists in the first place).
