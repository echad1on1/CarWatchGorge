package com.dashboard.core.service

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.hardware.AudioEvent
import com.dashboard.core.hardware.AudioOutput
import com.dashboard.core.hardware.Subscription

/**
 * Plays a beep as the driver approaches a camera, matching how the real Blizzer phone app
 * behaves. Deliberately separate from [BlizzerManager] and any overlay UI — mirrors
 * [NavigationAudioManager]'s split between "what's the current state" and "when does that
 * warrant a sound".
 *
 * Only fires on a genuinely new event id or a materially closer [BlizzerEvent.distanceMeters]
 * threshold, so a continuous camera-approach sequence (500m -> 200m -> 100m on the SAME event id,
 * per [com.dashboard.core.hardware.mock.MockPhoneCommunication.triggerCameraWarning]) beeps once
 * per threshold rather than once per any minor update.
 */
class BlizzerAudioManager(
    private val blizzerManager: BlizzerManager,
    private val audioOutput: AudioOutput,
) {
    private var lastAnnouncedId: String? = null
    private var lastAnnouncedDistance: Int? = null
    private var sub: Subscription? = null

    fun start() {
        sub = blizzerManager.observe { event -> onEvent(event) }
    }

    fun stop() {
        sub?.cancel()
        lastAnnouncedId = null
        lastAnnouncedDistance = null
    }

    private fun onEvent(event: BlizzerEvent?) {
        if (event == null) {
            lastAnnouncedId = null
            lastAnnouncedDistance = null
            return
        }
        val isNewEvent = event.id != lastAnnouncedId
        val isNewThreshold = event.distanceMeters != null && event.distanceMeters != lastAnnouncedDistance
        if (isNewEvent || isNewThreshold) {
            audioOutput.play(AudioEvent.BLIZZER_ALERT)
        }
        lastAnnouncedId = event.id
        lastAnnouncedDistance = event.distanceMeters
    }
}
