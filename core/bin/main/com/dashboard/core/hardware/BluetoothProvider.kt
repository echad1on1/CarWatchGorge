package com.dashboard.core.hardware

enum class LinkState { DISCONNECTED, CONNECTING, CONNECTED }

/**
 * Abstracts the physical Bluetooth/BLE transport used to reach the phone. This is deliberately
 * separate from [PhoneCommunication] (the protocol): Bluetooth is *only* the pipe. Swapping this
 * for a real BLE stack later must not require touching the communication protocol or any panel.
 */
interface BluetoothProvider {
    fun connect()
    fun disconnect()
    fun observeLinkState(listener: (LinkState) -> Unit): Subscription

    /** Raw bytes/frames in. The [com.dashboard.core.communication] layer decodes these into protocol messages. */
    fun observeRawInbound(listener: (ByteArray) -> Unit): Subscription

    /** Raw bytes/frames out. */
    fun send(data: ByteArray)
}
