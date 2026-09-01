package com.dashboard.wearos.hardware

import android.content.Context
import com.dashboard.core.hardware.BluetoothProvider
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.LinkState
import com.dashboard.core.hardware.Subscription
import com.dashboard.wearos.BuildConfig
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.atomic.AtomicReference

/**
 * Wear OS Data Layer transport implementing [BluetoothProvider]. Uses [MessageClient] for
 * outbound nav payloads and receives inbound messages via [NavDataListenerService.pushInbound].
 *
 * Shared by [com.dashboard.core.service.ConnectionManager] (link state from node presence) and
 * [com.dashboard.core.communication.BluetoothPhoneCommunication] (protocol bytes).
 */
class WearDataLayerBluetoothProvider private constructor(context: Context) : BluetoothProvider {

    private val appContext = context.applicationContext
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)

    private val linkStateEmitter = Emitter<LinkState>()
    private val inboundEmitter = Emitter<ByteArray>()
    private val state = AtomicReference(LinkState.DISCONNECTED)

    override fun connect() {
        if (state.get() != LinkState.DISCONNECTED) return
        setState(LinkState.CONNECTING)
        nodeClient.connectedNodes.addOnSuccessListener { nodes -> onNodesResolved(nodes) }
    }

    override fun disconnect() {
        setState(LinkState.DISCONNECTED)
    }

    override fun observeLinkState(listener: (LinkState) -> Unit): Subscription =
        linkStateEmitter.subscribe(listener)

    override fun observeRawInbound(listener: (ByteArray) -> Unit): Subscription =
        inboundEmitter.subscribe(listener)

    override fun send(data: ByteArray) {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(node.id, NAV_MESSAGE_PATH, data)
            }
        }
    }

    /** Called by [NavDataListenerService] when the phone sends a nav checkpoint. */
    fun pushInbound(data: ByteArray) {
        if (state.get() != LinkState.CONNECTED) {
            setState(LinkState.CONNECTED)
        }
        inboundEmitter.emit(data)
    }

    private fun onNodesResolved(nodes: List<Node>) {
        when {
            nodes.isNotEmpty() -> setState(LinkState.CONNECTED)
            BuildConfig.DEBUG -> setState(LinkState.CONNECTED) // emulator dev without a paired phone
            state.get() == LinkState.CONNECTING -> setState(LinkState.DISCONNECTED)
        }
    }

    private fun setState(newState: LinkState) {
        if (state.getAndSet(newState) == newState) return
        linkStateEmitter.emit(newState)
    }

    companion object {
        const val NAV_MESSAGE_PATH = "/automotive-dashboard/nav"

        @Volatile
        private var instance: WearDataLayerBluetoothProvider? = null

        fun getInstance(context: Context): WearDataLayerBluetoothProvider =
            instance ?: synchronized(this) {
                instance ?: WearDataLayerBluetoothProvider(context.applicationContext).also { instance = it }
            }
    }
}
