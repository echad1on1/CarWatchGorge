package com.dashboard.core.service

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.PhoneCommunication
import com.dashboard.core.hardware.Subscription
import java.util.Timer
import java.util.TimerTask

/**
 * Global overlay service. Deliberately NOT a panel: it has no notion of Car/Maps/Music at all.
 * It tracks "is there a currently-active Blizzer event, and what is it" — the UI shell renders
 * [currentEvent] as an overlay above whichever panel is on screen.
 *
 * Auto-dismisses [autoDismissMillis] after the most recent update for the current event (default
 * 5s, per spec) — this is a genuinely watch-side timeout, not dependent on the phone confirming
 * dismissal, since a real camera-proximity data source may never explicitly say "event over."
 * Any new update (even a distance change on the same event id, e.g. an approaching camera)
 * resets the timer, so it only fires after [autoDismissMillis] of silence.
 */
class BlizzerManager(
    private val phoneCommunication: PhoneCommunication,
    private val autoDismissMillis: Long = 5000,
) {
    private val emitter = Emitter<BlizzerEvent?>()

    var currentEvent: BlizzerEvent? = null
        private set

    private var sub: Subscription? = null
    private var dismissTimer: Timer? = null

    fun start() {
        sub = phoneCommunication.observeBlizzerEvents { event -> onEvent(event) }
    }

    fun stop() {
        sub?.cancel()
        dismissTimer?.cancel()
        dismissTimer = null
        currentEvent = null
    }

    /** listener receives null immediately if nothing is active, or the current event if one is. */
    fun observe(listener: (BlizzerEvent?) -> Unit): Subscription {
        listener(currentEvent)
        return emitter.subscribe(listener)
    }

    private fun onEvent(event: BlizzerEvent) {
        dismissTimer?.cancel()
        currentEvent = if (event.active) event else null
        emitter.emit(currentEvent)

        val activeId = currentEvent?.id ?: return
        dismissTimer = Timer("BlizzerAutoDismiss", true).apply {
            schedule(object : TimerTask() {
                override fun run() = autoDismiss(activeId)
            }, autoDismissMillis)
        }
    }

    /** Manually dismisses whatever event is currently active, canceling any pending auto-dismiss timer. */
    fun dismissCurrentEvent() {
        dismissTimer?.cancel()
        currentEvent = null
        emitter.emit(null)
    }

    private fun autoDismiss(id: String) {
        if (currentEvent?.id != id) return // a newer event already replaced this one
        currentEvent = null
        emitter.emit(null)
    }
}
