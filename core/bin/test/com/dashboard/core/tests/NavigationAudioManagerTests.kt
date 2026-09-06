package com.dashboard.core.tests

import com.dashboard.core.domain.Direction
import com.dashboard.core.hardware.AudioEvent
import com.dashboard.core.hardware.mock.MockAudioOutput
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.service.NavigationAudioManager
import com.dashboard.core.service.NavigationManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

fun navigationAudioManagerSuite() = TestSuite("NavigationAudioManager").apply {

    test("plays TURN_LEFT when navigation direction becomes TURN_LEFT") {
        val phone = MockPhoneCommunication()
        val navManager = NavigationManager(phone)
        val audio = MockAudioOutput()
        val audioManager = NavigationAudioManager(navManager, audio)
        navManager.start()
        audioManager.start()

        phone.startNavigation()
        phone.changeDirection(Direction.TURN_LEFT, 100.0)

        assertTrue(audio.playedEvents.contains(AudioEvent.TURN_LEFT), "should have played TURN_LEFT")
    }

    test("does not repeat the same direction's audio event on unrelated updates") {
        val phone = MockPhoneCommunication()
        val navManager = NavigationManager(phone)
        val audio = MockAudioOutput()
        val audioManager = NavigationAudioManager(navManager, audio)
        navManager.start()
        audioManager.start()

        phone.startNavigation()
        phone.changeDirection(Direction.TURN_RIGHT, 500.0)
        phone.decreaseDistance(100.0) // same direction, distance ticking down
        phone.decreaseDistance(100.0)

        val turnRightCount = audio.playedEvents.count { it == AudioEvent.TURN_RIGHT }
        assertEquals(1, turnRightCount, "should only announce TURN_RIGHT once, not on every distance update")
    }

    test("plays ARRIVED when navigation direction becomes ARRIVED") {
        val phone = MockPhoneCommunication()
        val navManager = NavigationManager(phone)
        val audio = MockAudioOutput()
        val audioManager = NavigationAudioManager(navManager, audio)
        navManager.start()
        audioManager.start()

        phone.startNavigation()
        phone.changeDirection(Direction.ARRIVED, 0.0)

        assertTrue(audio.playedEvents.contains(AudioEvent.ARRIVED), "should have played ARRIVED")
    }

    test("does not play anything for STRAIGHT") {
        val phone = MockPhoneCommunication()
        val navManager = NavigationManager(phone)
        val audio = MockAudioOutput()
        val audioManager = NavigationAudioManager(navManager, audio)
        navManager.start()
        audioManager.start()

        phone.startNavigation() // defaults to STRAIGHT

        assertTrue(audio.playedEvents.isEmpty(), "STRAIGHT should not trigger any audio event")
    }
}
