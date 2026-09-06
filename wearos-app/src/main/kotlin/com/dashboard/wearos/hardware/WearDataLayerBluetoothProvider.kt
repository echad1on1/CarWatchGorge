package com.dashboard.wearos.hardware

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
 * tested BluetoothPhoneCommunication with zero changes needed in core, and doubles as the
 * BluetoothProvider ConnectionManager uses for real link state.
 *
 * Singleton via [getInstance] so MainActivity and NavDataListenerService (which may be invoked
 * by the system in a fresh process, separate from any running Activity) always resolve to the
 * same instance's inbound emitter.
 */
class WearDataLayerBluetoothProvider private constructor(private val context: Context) : BluetoothProvider {

    companion object {
        @Volatile private var instance: WearDataLayerBluetoothProvider? = null

        fun getInstance(context: Context): WearDataLayerBluetoothProvider =
            instance ?: synchronized(this) {
                instance ?: WearDataLayerBluetoothProvider(context.applicationContext).also { instance = it }
            }

        /** Called by NavDataListenerService when a message arrives from the phone. */
        fun pushInbound(data: ByteArray) {
            instance?.inboundEmitter?.emit(data)
        }
    }

    private val inboundEmitter = Emitter<ByteArray>()
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
        inboundEmitter.subscribe(listener)

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
