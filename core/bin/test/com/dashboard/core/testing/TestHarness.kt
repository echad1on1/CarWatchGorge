package com.dashboard.core.testing

/**
 * Minimal assertion-based test harness.
 *
 * This sandbox has no access to Maven Central, so real JUnit5/Kotest can't be downloaded here.
 * Once this project is opened in Android Studio (with normal network access), swap this for
 * JUnit5 + kotlin-test — the test bodies below are written as plain functions so that migration
 * is a mechanical rename, not a rewrite.
 */
class AssertionFailedError(message: String) : Exception(message)

fun assertTrue(condition: Boolean, message: String) {
    if (!condition) throw AssertionFailedError(message)
}

fun assertEquals(expected: Any?, actual: Any?, message: String = "") {
    if (expected != actual) {
        throw AssertionFailedError("$message (expected=$expected, actual=$actual)")
    }
}

fun assertFalse(condition: Boolean, message: String) = assertTrue(!condition, message)

data class NamedTest(val name: String, val body: () -> Unit)

class TestSuite(val name: String, private val tests: MutableList<NamedTest> = mutableListOf()) {
    fun test(name: String, body: () -> Unit) {
        tests.add(NamedTest(name, body))
    }

    fun run(): Boolean {
        println("Suite: $name")
        var allPassed = true
        for (t in tests) {
            try {
                t.body()
                println("  PASS  ${t.name}")
            } catch (e: Exception) {
                allPassed = false
                println("  FAIL  ${t.name}: ${e.message}")
            }
        }
        return allPassed
    }
}
