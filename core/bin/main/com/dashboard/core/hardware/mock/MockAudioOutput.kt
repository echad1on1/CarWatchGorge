package com.dashboard.core.hardware.mock

import com.dashboard.core.hardware.AudioEvent
import com.dashboard.core.hardware.AudioOutput

/**
 * Placeholder audio sink for early development. Just records/logs what would have played.
 * This will be expanded in the NavigationAudioManager step (queuing, priority over Blizzer
 * alerts vs. turn-by-turn, etc.) and eventually replaced by a real BT-audio/AUX implementation.
 */
class MockAudioOutput : AudioOutput {
    private val _playedEvents = mutableListOf<AudioEvent>()
    val playedEvents: List<AudioEvent> get() = _playedEvents

    override fun play(event: AudioEvent) {
        _playedEvents.add(event)
        println("[MockAudioOutput] would play: $event")
    }
}
