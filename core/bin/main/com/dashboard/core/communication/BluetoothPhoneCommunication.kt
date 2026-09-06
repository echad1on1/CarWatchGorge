package com.dashboard.core.communication

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.MediaCommand
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.NavigationState
import com.dashboard.core.hardware.BluetoothProvider
import com.dashboard.core.hardware.PhoneCommunication
import com.dashboard.core.hardware.Subscription

/**
 * The real [PhoneCommunication] implementation. Decodes inbound bytes from any [BluetoothProvider]
 * into domain updates, and encodes outbound commands into bytes for it.
 *
 * This class is the concrete proof that "Bluetooth is only the transport": it depends on the
 * [BluetoothProvider] *interface*, not on Bluetooth itself, and it never touches raw bytes
 * outside of [MessageCodec]. A [com.dashboard.core.hardware.mock.MockBluetoothProvider] today or
 * a real BLE implementation later plug in here identically — nothing else in the app changes.
 */
class BluetoothPhoneCommunication(private val bluetoothProvider: BluetoothProvider) : PhoneCommunication {

    private val navigationListeners = mutableListOf<(NavigationState) -> Unit>()
    private val mediaListeners = mutableListOf<(MediaState) -> Unit>()
    private val blizzerListeners = mutableListOf<(BlizzerEvent) -> Unit>()

    init {
        bluetoothProvider.observeRawInbound { bytes -> onInbound(bytes) }
    }

    private fun onInbound(bytes: ByteArray) {
        val message = runCatching { MessageCodec.decode(bytes) }.getOrNull() ?: return
        when (message) {
            is ProtocolMessage.NavigationUpdate -> navigationListeners.forEach { it(message.toDomain()) }
            is ProtocolMessage.MediaUpdate -> mediaListeners.forEach { it(message.toDomain()) }
            is ProtocolMessage.BlizzerTrigger -> blizzerListeners.forEach { it(message.toDomain()) }
            // MediaCommandMessage, ConnectionUpdate, SettingsUpdate: not consumed on the dashboard
            // side today. ConnectionUpdate in particular is redundant with ConnectionManager, which
            // already derives state from BluetoothProvider's link state directly.
            else -> Unit
        }
    }

    override fun observeNavigationState(listener: (NavigationState) -> Unit): Subscription {
        navigationListeners.add(listener)
        return Subscription { navigationListeners.remove(listener) }
    }

    override fun observeMediaState(listener: (MediaState) -> Unit): Subscription {
        mediaListeners.add(listener)
        return Subscription { mediaListeners.remove(listener) }
    }

    override fun observeBlizzerEvents(listener: (BlizzerEvent) -> Unit): Subscription {
        blizzerListeners.add(listener)
        return Subscription { blizzerListeners.remove(listener) }
    }

    override fun sendMediaCommand(command: MediaCommand) {
        bluetoothProvider.send(MessageCodec.encode(command.toProtocol()))
    }
}
