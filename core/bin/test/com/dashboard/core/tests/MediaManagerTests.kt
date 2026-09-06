package com.dashboard.core.tests

import com.dashboard.core.domain.PlaybackState
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.service.MediaManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

fun mediaManagerSuite() = TestSuite("MediaManager").apply {

    test("starts with no media loaded") {
        val phone = MockPhoneCommunication()
        val manager = MediaManager(phone)
        manager.start()
        assertEquals(null, manager.latest.title, "should start with nothing playing")
    }

    test("reflects phone-side music updates") {
        val phone = MockPhoneCommunication()
        val manager = MediaManager(phone)
        manager.start()
        phone.startMusic()
        assertEquals(PlaybackState.PLAYING, manager.latest.playbackState, "should reflect PLAYING state")
    }

    test("play()/pause()/next()/previous() route through PhoneCommunication commands") {
        val phone = MockPhoneCommunication()
        val manager = MediaManager(phone)
        manager.start()
        phone.startMusic()
        val firstTitle = manager.latest.title

        manager.pause()
        assertEquals(PlaybackState.PAUSED, manager.latest.playbackState, "pause() should pause")

        manager.play()
        assertEquals(PlaybackState.PLAYING, manager.latest.playbackState, "play() should resume")

        manager.next()
        assertTrue(manager.latest.title != firstTitle, "next() should advance the track")
    }
}
