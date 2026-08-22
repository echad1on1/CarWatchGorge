package com.dashboard.core.tests

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.Direction
import com.dashboard.core.domain.MediaCommand
import com.dashboard.core.domain.PlaybackState
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertFalse
import com.dashboard.core.testing.assertTrue

fun mockPhoneCommunicationSuite() = TestSuite("MockPhoneCommunication dev controls").apply {

    test("navigation starts inactive") {
        val phone = MockPhoneCommunication()
        assertFalse(phone.currentNavigation.active, "should start with no navigation running")
    }

    test("startNavigation activates navigation with a road and ETA") {
        val phone = MockPhoneCommunication()
        phone.startNavigation(roadName = "Coast Hwy", etaMinutes = 15)
        assertTrue(phone.currentNavigation.active, "navigation should be active")
        assertEquals("Coast Hwy", phone.currentNavigation.roadName, "road name should be set")
        assertEquals(15, phone.currentNavigation.etaMinutes, "eta should be set")
    }

    test("changeDirection updates direction and distance while navigating") {
        val phone = MockPhoneCommunication()
        phone.startNavigation()
        phone.changeDirection(Direction.TURN_RIGHT, distanceMeters = 120.0)
        assertEquals(Direction.TURN_RIGHT, phone.currentNavigation.direction, "direction should update")
        assertEquals(120.0, phone.currentNavigation.distanceMeters, "distance should update")
    }

    test("changeDirection is a no-op when navigation is inactive") {
        val phone = MockPhoneCommunication()
        phone.changeDirection(Direction.TURN_RIGHT, distanceMeters = 120.0)
        assertFalse(phone.currentNavigation.active, "should remain inactive")
    }

    test("decreaseDistance reduces remaining distance and floors at zero") {
        val phone = MockPhoneCommunication()
        phone.startNavigation()
        phone.changeDirection(Direction.STRAIGHT, distanceMeters = 50.0)
        phone.decreaseDistance(80.0)
        assertEquals(0.0, phone.currentNavigation.distanceMeters, "distance should floor at 0, never go negative")
    }

    test("stopNavigation returns to inactive") {
        val phone = MockPhoneCommunication()
        phone.startNavigation()
        phone.stopNavigation()
        assertFalse(phone.currentNavigation.active, "should be inactive after stopNavigation")
    }

    test("startMusic begins playback of the first song") {
        val phone = MockPhoneCommunication()
        phone.startMusic()
        assertEquals(PlaybackState.PLAYING, phone.currentMedia.playbackState, "should be playing")
        assertTrue(phone.currentMedia.title != null, "should have a song loaded")
    }

    test("pauseMusic via sendMediaCommand pauses playback") {
        val phone = MockPhoneCommunication()
        phone.startMusic()
        phone.sendMediaCommand(MediaCommand.PAUSE)
        assertEquals(PlaybackState.PAUSED, phone.currentMedia.playbackState, "should be paused")
    }

    test("nextSong via sendMediaCommand advances the track and keeps playing") {
        val phone = MockPhoneCommunication()
        phone.startMusic()
        val firstTitle = phone.currentMedia.title
        phone.sendMediaCommand(MediaCommand.NEXT)
        assertTrue(phone.currentMedia.title != firstTitle, "title should change after NEXT")
        assertEquals(PlaybackState.PLAYING, phone.currentMedia.playbackState, "should still be playing")
    }

    test("previousSong via sendMediaCommand wraps around at the start of the library") {
        val phone = MockPhoneCommunication()
        phone.startMusic()
        val firstTitle = phone.currentMedia.title
        phone.sendMediaCommand(MediaCommand.PREVIOUS)
        assertTrue(phone.currentMedia.title != firstTitle, "should wrap to the last song, not stay on the first")
    }

    test("triggerBlizzer emits an active event to subscribers") {
        val phone = MockPhoneCommunication()
        var received: BlizzerEvent? = null
        phone.observeBlizzerEvents { received = it }
        phone.triggerBlizzer("Low tire pressure")
        assertTrue(received != null, "listener should receive the event")
        assertTrue(received!!.active, "event should be marked active")
        assertEquals("Low tire pressure", received!!.message, "message should round-trip")
    }

    test("late subscriber to navigation state gets current snapshot immediately") {
        val phone = MockPhoneCommunication()
        phone.startNavigation(roadName = "Elm St")
        var received: String? = null
        phone.observeNavigationState { received = it.roadName }
        assertEquals("Elm St", received, "late subscriber should see current nav state immediately")
    }
}
