package com.dashboard.core.hardware.mock

import com.dashboard.core.hardware.BluetoothProvider
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.LinkState
import com.dashboard.core.hardware.Subscription
import java.util.Timer
import java.util.TimerTask

/**
 * Development stand-in for real Bluetooth/BLE. Simulates a brief CONNECTING phase before
 * CONNECTED, mirroring how a real Bluetooth pairing/handshake takes a moment — this keeps the
 * [com.dashboard.core.service.ConnectionManager] state machine honest even in simulation.
 */
class MockBluetoothProvider(private val connectDelayMillis: Long = 400) : BluetoothProvider {
    private val linkStateEmitter = Emitter<LinkState>()
    private val inboundEmitter = Emitter<ByteArray>()
    private var state = LinkState.DISCONNECTED
    private val timer = Timer("MockBluetoothProvider", true)

    override fun connect() {
        if (state != LinkState.DISCONNECTED) return
        setState(LinkState.CONNECTING)
        timer.schedule(object : TimerTask() {
            override fun run() { setState(LinkState.CONNECTED) }
        }, connectDelayMillis)
    }

    override fun disconnect() {
        setState(LinkState.DISCONNECTED)
    }

    override fun observeLinkState(listener: (LinkState) -> Unit): Subscription =
        linkStateEmitter.subscribe(listener)

    override fun observeRawInbound(listener: (ByteArray) -> Unit): Subscription =
        inboundEmitter.subscribe(listener)

    override fun send(data: ByteArray) {
        // No real transport yet; a future step's MockPhoneCommunication can loop this back
        // to simulate a phone response if needed.
    }

    private fun setState(newState: LinkState) {
        state = newState
        linkStateEmitter.emit(newState)
    }
}
