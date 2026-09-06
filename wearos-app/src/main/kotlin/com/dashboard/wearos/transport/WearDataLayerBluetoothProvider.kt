package com.dashboard.wearos.transport

import android.content.Context
import com.dashboard.core.hardware.BluetoothProvider
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.LinkState
import com.dashboard.core.hardware.Subscription
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable

/** Must match the path phone-app's WearMessageSender sends to. */
const val MESSAGE_PATH = "/automotive-dashboard/nav"

/**
 * The real BluetoothProvider implementation for the watch side, backed by the Wear OS Data
 * Layer API (MessageClient/NodeClient), NOT raw Bluetooth sockets — per Google's own guidance
 * (see docs/android-integration-research.md). Plugs directly into core's existing, already-
 * tested BluetoothPhoneCommunication with zero changes needed in core.
 *
 * Inbound bytes arrive via NavDataListenerService (a WearableListenerService, invoked by the
 * system even when this app isn't foregrounded) and are pushed into sharedInboundEmitter, a
 * process-wide emitter every instance of this class subscribes to.
 */
class WearDataLayerBluetoothProvider(private val context: Context) : BluetoothProvider {

    companion object {
        val sharedInboundEmitter = Emitter<ByteArray>()

        fun pushInbound(data: ByteArray) {
            sharedInboundEmitter.emit(data)
        }
    }

    private val linkStateEmitter = Emitter<LinkState>()
    private var state = LinkState.DISCONNECTED

    override fun connect() {
        setState(LinkState.CONNECTING)
        try {
            val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
            setState(if (nodes.isNotEmpty()) LinkState.CONNECTED else LinkState.DISCONNECTED)
        } catch (e: Exception) {
            setState(LinkState.DISCONNECTED)
        }
    }

    override fun disconnect() {
        setState(LinkState.DISCONNECTED)
    }

    override fun observeLinkState(listener: (LinkState) -> Unit): Subscription =
        linkStateEmitter.subscribe(listener)

    override fun observeRawInbound(listener: (ByteArray) -> Unit): Subscription =
        sharedInboundEmitter.subscribe(listener)

    override fun send(data: ByteArray) {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, MESSAGE_PATH, data)
            }
        }
    }

    private fun setState(newState: LinkState) {
        if (state == newState) return
        state = newState
        linkStateEmitter.emit(newState)
    }
}
