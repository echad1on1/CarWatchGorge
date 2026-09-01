package com.dashboard.core.tests

fun main() {
    val suites = listOf(
        vehicleDataSuite(),
        vehicleDataManagerSuite(),
        connectionManagerSuite(),
        messageCodecSuite(),
        mockPhoneCommunicationSuite(),
        bluetoothPhoneCommunicationSuite(),
        navigationManagerSuite(),
        navigationAnnouncementParserSuite(),
        navigationCountdownSuite(),
        navigationAudioManagerSuite(),
        mediaManagerSuite(),
        blizzerManagerSuite(),
        blizzerProximitySuite(),
        blizzerAudioManagerSuite(),
        settingsManagerSuite(),
        powerManagerSuite(),
        endToEndJourneySuite(),
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
