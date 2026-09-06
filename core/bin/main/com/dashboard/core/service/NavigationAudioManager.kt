package com.dashboard.core.service

import com.dashboard.core.domain.Direction
import com.dashboard.core.domain.NavigationState
import com.dashboard.core.hardware.AudioEvent
import com.dashboard.core.hardware.AudioOutput
import com.dashboard.core.hardware.Subscription

/**
 * Turns navigation state changes into spoken/audio turn-by-turn cues. Deliberately separate from
 * the Maps UI (per spec: "Navigation sounds must not be hardcoded into the Maps UI") — the panel
 * only ever renders [NavigationState]; this manager independently listens to the same stream and
 * decides when a *new* direction warrants a sound.
 *
 * Only fires on an actual change of [NavigationState.direction] while navigation is active, so
 * unrelated updates (e.g. distance ticking down) don't repeat the same instruction.
 */
class NavigationAudioManager(
    private val navigationManager: NavigationManager,
    private val audioOutput: AudioOutput,
) {
    private var lastAnnouncedDirection: Direction? = null
    private var sub: Subscription? = null

    fun start() {
        sub = navigationManager.observe { state -> onNavigationUpdate(state) }
    }

    fun stop() {
        sub?.cancel()
        lastAnnouncedDirection = null
    }

    private fun onNavigationUpdate(state: NavigationState) {
        if (!state.active) {
            lastAnnouncedDirection = null
            return
        }
        if (state.direction == lastAnnouncedDirection) return
        lastAnnouncedDirection = state.direction

        directionToAudioEvent(state.direction)?.let { audioOutput.play(it) }
    }

    private fun directionToAudioEvent(direction: Direction): AudioEvent? = when (direction) {
        Direction.TURN_LEFT -> AudioEvent.TURN_LEFT
        Direction.TURN_RIGHT -> AudioEvent.TURN_RIGHT
        Direction.KEEP_LEFT -> AudioEvent.KEEP_LEFT
        Direction.KEEP_RIGHT -> AudioEvent.KEEP_RIGHT
        Direction.ROUNDABOUT -> AudioEvent.ROUNDABOUT
        Direction.ARRIVED -> AudioEvent.ARRIVED
        Direction.STRAIGHT, Direction.UNKNOWN -> null // nothing worth announcing
    }
}
