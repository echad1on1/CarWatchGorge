package com.dashboard.core.hardware

/** A sound the dashboard needs the car stereo to play. Navigation and Blizzer are the first producers. */
enum class AudioEvent {
    TURN_LEFT, TURN_RIGHT, KEEP_LEFT, KEEP_RIGHT, ROUNDABOUT, ARRIVED,
    BLIZZER_ALERT,
}

/**
 * Abstracts wherever sound ultimately comes out: Bluetooth audio today, a future AUX/cable
 * adapter for cars without Bluetooth audio, or the dashboard's own speaker as a last resort.
 * Callers never touch an audio API directly — they just play an [AudioEvent].
 */
interface AudioOutput {
    fun play(event: AudioEvent)
}
