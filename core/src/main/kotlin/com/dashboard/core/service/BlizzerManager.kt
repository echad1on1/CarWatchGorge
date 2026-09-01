package com.dashboard.core.service

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.PhoneCommunication
import com.dashboard.core.hardware.Subscription

/**
 * Global overlay service. Deliberately NOT a panel: it has no notion of Car/Maps/Music at all.
 * It just tracks "is there a currently-active Blizzer event, and what is it". The UI shell is
 * responsible for rendering [currentEvent] as an overlay above whichever panel is on screen, and
 * for returning to that exact panel once [currentEvent] goes back to null — Blizzer itself never
 * needs to know which panel that was, which is what makes it work identically over Car, Maps or
 * Music without any panel-specific code.
 *
 * An event ends when the phone reports the same event id again with `active = false`
 * ([com.dashboard.core.hardware.mock.MockPhoneCommunication.dismissBlizzer] simulates this).
 */
class BlizzerManager(private val phoneCommunication: PhoneCommunication) {

    private val emitter = Emitter<BlizzerEvent?>()

    /** The event currently overlaying the UI, or null if nothing is active. */
    var currentEvent: BlizzerEvent? = null
        private set

    private var sub: Subscription? = null

    fun start() {
        sub = phoneCommunication.observeBlizzerEvents { event -> onEvent(event) }
    }

    fun stop() {
        sub?.cancel()
        currentEvent = null
    }

    /** listener receives null immediately if nothing is active, or the current event if one is. */
    fun observe(listener: (BlizzerEvent?) -> Unit): Subscription {
        listener(currentEvent)
        return emitter.subscribe(listener)
    }

    /** Clears the active overlay (e.g. after the 5-second auto-dismiss timer). */
    fun dismissCurrentEvent() {
        if (currentEvent == null) return
        currentEvent = null
        emitter.emit(null)
    }

    private fun onEvent(event: BlizzerEvent) {
        currentEvent = if (event.active) event else null
        emitter.emit(currentEvent)
    }
}
