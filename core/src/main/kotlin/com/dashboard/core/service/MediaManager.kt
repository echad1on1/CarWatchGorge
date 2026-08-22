package com.dashboard.core.service

import com.dashboard.core.domain.MediaCommand
import com.dashboard.core.domain.MediaState
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.PhoneCommunication
import com.dashboard.core.hardware.Subscription

/**
 * The Music panel's ONLY dependency. Mirrors [VehicleDataManager]/[NavigationManager]: caches
 * the latest snapshot for late subscribers, and turns play/pause/next/previous into
 * [PhoneCommunication.sendMediaCommand] calls so the panel never needs to know the command enum
 * exists — it just calls [play], [pause], [next], [previous].
 *
 * No dependency on Spotify, Apple Music, or any specific media app — whatever is playing on the
 * phone is reported through the same [MediaState] shape.
 */
class MediaManager(private val phoneCommunication: PhoneCommunication) {

    private val emitter = Emitter<MediaState>()

    var latest: MediaState = MediaState.NONE
        private set

    private var sub: Subscription? = null

    fun start() {
        sub = phoneCommunication.observeMediaState { state ->
            latest = state
            emitter.emit(state)
        }
    }

    fun stop() {
        sub?.cancel()
        latest = MediaState.NONE
    }

    fun observe(listener: (MediaState) -> Unit): Subscription {
        listener(latest)
        return emitter.subscribe(listener)
    }

    fun play() = phoneCommunication.sendMediaCommand(MediaCommand.PLAY)
    fun pause() = phoneCommunication.sendMediaCommand(MediaCommand.PAUSE)
    fun next() = phoneCommunication.sendMediaCommand(MediaCommand.NEXT)
    fun previous() = phoneCommunication.sendMediaCommand(MediaCommand.PREVIOUS)
}
