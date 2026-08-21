package com.dashboard.core.hardware

/**
 * Abstracts the physical NFC reader. A real implementation will react to an actual tag/phone
 * tap; [com.dashboard.core.hardware.mock.MockNfcProvider] exposes a `simulateTap()` dev hook
 * that fires the exact same callback a real tap will later fire.
 *
 * Nothing in the UI or service layer should know whether a tap is real or simulated.
 */
interface NfcProvider {
    fun start()
    fun stop()

    /** Fired the moment a phone tap is detected. Carries no payload yet — just "a tap happened". */
    fun onTapDetected(listener: () -> Unit): Subscription
}
