package com.dashboard.core.service

import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.hardware.BluetoothProvider
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.LinkState
import com.dashboard.core.hardware.NfcProvider
import com.dashboard.core.hardware.Subscription

/**
 * Owns the phone-connection state machine described in the spec:
 *
 * CAR_ONLY --(NFC tap)--> NFC_DETECTED --(auto)--> CONNECTING --(BT connected)--> CONNECTED
 * CONNECTED --(BT disconnected)--> DISCONNECTING --(auto)--> CAR_ONLY
 * any in-progress state --(BT/NFC failure)--> ERROR --(auto)--> CAR_ONLY
 *
 * This is the ONLY place that decides what state the dashboard is in. Panels and UI observe
 * [state] (or [observeState]) — they never touch [NfcProvider] or [BluetoothProvider] directly.
 *
 * Depends only on the [NfcProvider] and [BluetoothProvider] *interfaces*, so a mock and a real
 * implementation are interchangeable without any change here.
 */
class ConnectionManager(
    private val nfcProvider: NfcProvider,
    private val bluetoothProvider: BluetoothProvider,
) {
    private val stateEmitter = Emitter<ConnectionState>()

    var state: ConnectionState = ConnectionState.CAR_ONLY
        private set(value) {
            if (field == value) return
            field = value
            stateEmitter.emit(value)
        }

    private var nfcSub: Subscription? = null
    private var btSub: Subscription? = null

    /** Wires up listeners to the hardware interfaces. Call once during app startup. */
    fun start() {
        nfcProvider.start()
        nfcSub = nfcProvider.onTapDetected { onNfcTap() }
        btSub = bluetoothProvider.observeLinkState { onLinkState(it) }
    }

    fun stop() {
        nfcSub?.cancel()
        btSub?.cancel()
        nfcProvider.stop()
    }

    fun observeState(listener: (ConnectionState) -> Unit): Subscription {
        // Immediately deliver current state so a late subscriber isn't stuck waiting for a change.
        listener(state)
        return stateEmitter.subscribe(listener)
    }

    /** Explicit developer entry point mirrors what NFC hardware triggers, but is state-machine safe either way. */
    private fun onNfcTap() {
        if (state != ConnectionState.CAR_ONLY) return // ignore taps mid-connection or while connected
        state = ConnectionState.NFC_DETECTED
        state = ConnectionState.CONNECTING
        bluetoothProvider.connect()
    }

    private fun onLinkState(linkState: LinkState) {
        when (linkState) {
            LinkState.CONNECTED -> {
                if (state == ConnectionState.CONNECTING) {
                    state = ConnectionState.CONNECTED
                }
            }
            LinkState.DISCONNECTED -> {
                if (state == ConnectionState.CONNECTED) {
                    state = ConnectionState.DISCONNECTING
                    state = ConnectionState.CAR_ONLY
                } else if (state == ConnectionState.CONNECTING || state == ConnectionState.NFC_DETECTED) {
                    // Link dropped before we ever reached CONNECTED.
                    state = ConnectionState.ERROR
                    state = ConnectionState.CAR_ONLY
                }
            }
            LinkState.CONNECTING -> Unit // already reflected via CONNECTING above
        }
    }

    /** Developer control: force a disconnect, exactly as a real BT drop would. */
    fun simulateDisconnect() {
        bluetoothProvider.disconnect()
    }
}
