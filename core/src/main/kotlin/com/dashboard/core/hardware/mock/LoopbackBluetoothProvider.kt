package com.dashboard.core.hardware.mock

import com.dashboard.core.hardware.BluetoothProvider
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.LinkState
import com.dashboard.core.hardware.Subscription

/**
 * Two [LoopbackBluetoothProvider]s wired together with [pair] genuinely exchange bytes —
 * `a.send(x)` causes `b`'s `observeRawInbound` to fire with `x`, and vice versa. This is what
 * makes [com.dashboard.core.communication.BluetoothPhoneCommunication] a *provable* proof that
 * the protocol doesn't depend on real Bluetooth: the same class talks across this loopback pair
 * exactly as it would across a real BLE link.
 *
 * Used to represent "the dashboard's side" and "the phone's side" of the link in tests and in
 * the console demo, standing in for the not-yet-built real phone.
 */
class LoopbackBluetoothProvider : BluetoothProvider {
    private var peer: LoopbackBluetoothProvider? = null
    private val linkStateEmitter = Emitter<LinkState>()
    private val inboundEmitter = Emitter<ByteArray>()
    private var state = LinkState.DISCONNECTED

    companion object {
        /** Creates two endpoints wired to each other. */
        fun pair(): Pair<LoopbackBluetoothProvider, LoopbackBluetoothProvider> {
            val a = LoopbackBluetoothProvider()
            val b = LoopbackBluetoothProvider()
            a.peer = b
            b.peer = a
            return a to b
        }
    }

    override fun connect() {
        setState(LinkState.CONNECTING)
        setState(LinkState.CONNECTED)
        peer?.let { if (it.state != LinkState.CONNECTED) it.connect() }
    }

    override fun disconnect() {
        setState(LinkState.DISCONNECTED)
    }

    override fun observeLinkState(listener: (LinkState) -> Unit): Subscription =
        linkStateEmitter.subscribe(listener)

    override fun observeRawInbound(listener: (ByteArray) -> Unit): Subscription =
        inboundEmitter.subscribe(listener)

    override fun send(data: ByteArray) {
        peer?.receive(data)
    }

    private fun receive(data: ByteArray) {
        inboundEmitter.emit(data)
    }

    private fun setState(newState: LinkState) {
        if (state == newState) return
        state = newState
        linkStateEmitter.emit(newState)
    }
}
