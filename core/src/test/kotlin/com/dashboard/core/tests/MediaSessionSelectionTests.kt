package com.dashboard.core.tests

import com.dashboard.core.communication.MediaSessionSelection
import com.dashboard.core.communication.SessionSnapshot
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.PlaybackState
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

private fun snap(pkg: String, playback: PlaybackState, lastActive: Long) = SessionSnapshot(
    packageName = pkg,
    state = MediaState(title = pkg, playbackState = playback),
    lastActiveMillis = lastActive,
)

fun mediaSessionSelectionSuite() = TestSuite("MediaSessionSelection").apply {

    test("no sessions -> null") {
        assertEquals(null, MediaSessionSelection.chooseActive(emptyList()))
    }

    test("single session is chosen regardless of playback state") {
        val only = snap("com.podcast", PlaybackState.PAUSED, 100)
        assertEquals(only, MediaSessionSelection.chooseActive(listOf(only)))
    }

    test("a PLAYING session beats a more-recent PAUSED one") {
        val playing = snap("com.radio", PlaybackState.PLAYING, 100)
        val pausedNewer = snap("com.podcast", PlaybackState.PAUSED, 999)
        assertEquals(playing, MediaSessionSelection.chooseActive(listOf(pausedNewer, playing)))
    }

    test("two PLAYING sessions -> most recently active wins") {
        val older = snap("com.a", PlaybackState.PLAYING, 100)
        val newer = snap("com.b", PlaybackState.PLAYING, 500)
        assertEquals(newer, MediaSessionSelection.chooseActive(listOf(older, newer)))
    }

    test("PAUSED beats STOPPED and UNKNOWN") {
        val paused = snap("com.paused", PlaybackState.PAUSED, 10)
        val stopped = snap("com.stopped", PlaybackState.STOPPED, 900)
        val unknown = snap("com.unknown", PlaybackState.UNKNOWN, 900)
        assertEquals(paused, MediaSessionSelection.chooseActive(listOf(stopped, unknown, paused)))
    }

    test("selection is stable / deterministic for repeated calls") {
        val list = listOf(
            snap("com.a", PlaybackState.PAUSED, 5),
            snap("com.b", PlaybackState.PLAYING, 5),
            snap("com.c", PlaybackState.PLAYING, 5),
        )
        val first = MediaSessionSelection.chooseActive(list)
        assertTrue(first != null && first.state.playbackState == PlaybackState.PLAYING, "picks a playing one")
        assertEquals(first, MediaSessionSelection.chooseActive(list))
    }
}
