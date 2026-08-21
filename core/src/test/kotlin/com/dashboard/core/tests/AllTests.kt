package com.dashboard.core.tests

fun main() {
    val suites = listOf(
        vehicleDataSuite(),
        vehicleDataManagerSuite(),
        connectionManagerSuite(),
    )

    var allPassed = true
    for (suite in suites) {
        if (!suite.run()) allPassed = false
        println()
    }

    if (!allPassed) {
        println("SOME TESTS FAILED")
        kotlin.system.exitProcess(1)
    } else {
        println("ALL TESTS PASSED")
    }
}
