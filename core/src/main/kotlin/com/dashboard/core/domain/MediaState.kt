package com.dashboard.core.domain

enum class PlaybackState { PLAYING, PAUSED, STOPPED, UNKNOWN }

/**
 * What the Music panel shows/controls. Sourced from whatever media app is playing on the
 * phone — the dashboard has no direct dependency on Spotify, Apple Music, etc.
 */
data class MediaState(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val playbackState: PlaybackState = PlaybackState.UNKNOWN,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
) {
    companion object {
        val NONE = MediaState()
    }
}

/** Commands the dashboard can send back to the phone. Transport-independent. */
enum class MediaCommand { PLAY, PAUSE, NEXT, PREVIOUS }
