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

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

// Repositories are declared once, project-wide, in settings.gradle.kts — Gradle doesn't allow
// declaring them again per-module.

// No dependencies today. If JUnit5 becomes available once this is opened in an environment with
// normal Maven access, add it here as a testImplementation and migrate
// src/test/kotlin/.../testing/TestHarness.kt's callers over (see its class doc for why it
// exists in the first place).

// Runs the hand-rolled test harness (core/src/test/.../tests/AllTests.kt has the `main`) through
// Gradle, so CI and machines without a standalone `kotlinc` on PATH can run it too — the
// `tools/run_tests.sh` script still works for a quick local run. Zero new dependencies: the
// Kotlin plugin already puts kotlin-stdlib on the test runtime classpath.
tasks.register<JavaExec>("runCoreTests") {
    group = "verification"
    description = "Compiles and runs the core hand-rolled test suite (AllTests.main)."
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.dashboard.core.tests.AllTestsKt")
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.named("check") { dependsOn("runCoreTests") }
