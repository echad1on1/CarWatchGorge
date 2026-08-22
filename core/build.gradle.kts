// `core` is deliberately a plain Kotlin/JVM module, not an Android library module — it has zero
// Android dependency (see README's "Target platform decision" section), which is what makes it
// portable to whatever the final UI toolkit turns out to be. wearos-app depends on this module
// like any other Kotlin library.
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

// No dependencies today. If JUnit5 becomes available once this is opened in an environment with
// normal Maven access, add it here as a testImplementation and migrate
// src/test/kotlin/.../testing/TestHarness.kt's callers over (see its class doc for why it
// exists in the first place).
