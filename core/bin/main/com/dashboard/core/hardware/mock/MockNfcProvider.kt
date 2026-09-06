package com.dashboard.core.hardware.mock

import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.NfcProvider
import com.dashboard.core.hardware.Subscription

/**
 * Development stand-in for the real NFC reader. [simulateTap] is the "Simulate NFC Tap"
 * developer control described in the spec — it fires [onTapDetected] exactly the way a real
 * tap will, so nothing downstream needs to change when real NFC hardware is wired in.
 */
class MockNfcProvider : NfcProvider {
    private val tapEmitter = Emitter<Unit>()
    private var running = false

    override fun start() { running = true }
    override fun stop() { running = false }

    override fun onTapDetected(listener: () -> Unit): Subscription =
        tapEmitter.subscribe { listener() }

    /** Developer control: simulate a phone tap. No-op if not started (mirrors a real reader). */
    fun simulateTap() {
        if (!running) return
        tapEmitter.emit(Unit)
    }
}
