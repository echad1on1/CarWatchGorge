package com.dashboard.core.tests

import com.dashboard.core.domain.VehicleData
import com.dashboard.core.hardware.BluetoothProvider
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.LinkState
import com.dashboard.core.hardware.Subscription
import com.dashboard.core.hardware.VehicleDataProvider

/** Deterministic VehicleDataProvider for tests: emits only when [push] is called. */
class FakeVehicleDataProvider : VehicleDataProvider {
    private val emitter = Emitter<VehicleData>()
    var started = false
        private set

    override fun start() { started = true }
    override fun stop() { started = false }
    override fun observe(listener: (VehicleData) -> Unit): Subscription = emitter.subscribe(listener)

    fun push(data: VehicleData) = emitter.emit(data)
}

/**
 * Deterministic BluetoothProvider for tests: connect()/disconnect() transition state
 * synchronously (no timer delay like [com.dashboard.core.hardware.mock.MockBluetoothProvider]),
 * and [failToConnect] lets tests exercise the ERROR path.
 */
class FakeBluetoothProvider : BluetoothProvider {
    private val linkStateEmitter = Emitter<LinkState>()
    private val inboundEmitter = Emitter<ByteArray>()
    var state = LinkState.DISCONNECTED
        private set
    var failNextConnect = false

    override fun connect() {
        if (failNextConnect) {
            failNextConnect = false
            setState(LinkState.CONNECTING)
            setState(LinkState.DISCONNECTED) // simulates a failed handshake
            return
        }
        setState(LinkState.CONNECTING)
        setState(LinkState.CONNECTED)
    }

    override fun disconnect() = setState(LinkState.DISCONNECTED)

    override fun observeLinkState(listener: (LinkState) -> Unit): Subscription =
        linkStateEmitter.subscribe(listener)

    override fun observeRawInbound(listener: (ByteArray) -> Unit): Subscription =
        inboundEmitter.subscribe(listener)

    override fun send(data: ByteArray) {}

    private fun setState(newState: LinkState) {
        state = newState
        linkStateEmitter.emit(newState)
    }
}
