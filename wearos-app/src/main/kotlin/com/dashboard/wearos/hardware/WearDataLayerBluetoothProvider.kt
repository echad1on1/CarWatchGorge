package com.dashboard.wearos.hardware

import android.content.Context
import com.dashboard.core.hardware.BluetoothProvider
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.LinkState
import com.dashboard.core.hardware.Subscription
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

/** Must match the path phone-app's WearMessageSender sends to (any sub-path under the prefix). */
const val MESSAGE_PATH_PREFIX = "/automotive-dashboard"
const val MESSAGE_PATH = "$MESSAGE_PATH_PREFIX/nav"

/**
 * Capability the phone companion declares it `provides` (see phone-app `res/values/wear.xml`).
 * The watch resolves this to find the right node instead of broadcasting to every connected
 * device, and watches it to know when the phone appears/disappears.
 */
private const val PHONE_CAPABILITY = "automotive_dashboard_phone"

/**
 * The real [BluetoothProvider] for the watch side, backed by the Wear OS Data Layer API
 * (MessageClient/CapabilityClient), NOT raw Bluetooth sockets — per Google's guidance (see
 * docs/android-integration-research.md). Plugs into core's already-tested
 * BluetoothPhoneCommunication with zero core changes, and doubles as the BluetoothProvider
 * ConnectionManager uses for real link state.
 *
 * Everything here is asynchronous (Play-services `Task` callbacks) — [connect] never blocks the
 * caller's thread, so it is safe to call straight from `ConnectionManager.onNfcTap()` on the
 * main thread. Link state is driven by a live [CapabilityClient] listener, so a later phone
 * connect/disconnect is reflected without another `connect()` call.
 *
 * Singleton via [getInstance] so MainActivity and [NavDataListenerService] (which the system
 * may start in a fresh process) resolve to the same inbound emitter. A small replay buffer
 * covers the cold-start race where a message arrives before core has wired up its subscriber.
 */
class WearDataLayerBluetoothProvider private constructor(context: Context) : BluetoothProvider {

    companion object {
        @Volatile private var instance: WearDataLayerBluetoothProvider? = null

        fun getInstance(context: Context): WearDataLayerBluetoothProvider =
            instance ?: synchronized(this) {
                instance ?: WearDataLayerBluetoothProvider(context.applicationContext).also { instance = it }
            }

        /** Called by [NavDataListenerService] when the app process was not already alive. */
        fun pushInbound(context: Context, data: ByteArray) {
            getInstance(context).deliverInbound(data)
        }
    }

    private val appContext = context.applicationContext
    private val inboundEmitter = Emitter<ByteArray>()
    private val linkStateEmitter = Emitter<LinkState>()

    private var state = LinkState.DISCONNECTED
    private var started = false

    /** Last few inbound frames, replayed to each new subscriber so a cold-start message isn't lost. */
    private val replayBuffer = ArrayDeque<ByteArray>()
    private val replayCapacity = 8

    private val messageListener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
        if (event.path.startsWith(MESSAGE_PATH_PREFIX)) deliverInbound(event.data)
    }

    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener { info: CapabilityInfo ->
        setState(if (info.nodes.isNotEmpty()) LinkState.CONNECTED else LinkState.DISCONNECTED)
    }

    override fun connect() {
        setState(LinkState.CONNECTING)
        if (!started) {
            started = true
            Wearable.getMessageClient(appContext).addListener(messageListener)
            Wearable.getCapabilityClient(appContext)
                .addListener(capabilityListener, PHONE_CAPABILITY)
        }
        // Seed current link state without blocking.
        Wearable.getCapabilityClient(appContext)
            .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { info ->
                setState(if (info.nodes.isNotEmpty()) LinkState.CONNECTED else LinkState.DISCONNECTED)
            }
            .addOnFailureListener {
                // Fall back to any connected node if the capability isn't published yet.
                Wearable.getNodeClient(appContext).connectedNodes
                    .addOnSuccessListener { nodes ->
                        setState(if (nodes.isNotEmpty()) LinkState.CONNECTED else LinkState.DISCONNECTED)
                    }
                    .addOnFailureListener { setState(LinkState.DISCONNECTED) }
            }
    }

    override fun disconnect() {
        if (started) {
            started = false
            Wearable.getMessageClient(appContext).removeListener(messageListener)
            Wearable.getCapabilityClient(appContext).removeListener(capabilityListener)
        }
        setState(LinkState.DISCONNECTED)
    }

    override fun observeLinkState(listener: (LinkState) -> Unit): Subscription =
        linkStateEmitter.subscribe(listener)

    override fun observeRawInbound(listener: (ByteArray) -> Unit): Subscription {
        val sub = inboundEmitter.subscribe(listener)
        // Replay anything buffered before this subscriber existed.
        replayBuffer.toList().forEach(listener)
        return sub
    }

    override fun send(data: ByteArray) {
        resolveTargetNodes { nodes ->
            val client = Wearable.getMessageClient(appContext)
            nodes.forEach { node -> client.sendMessage(node.id, MESSAGE_PATH, data) }
        }
    }

    private fun deliverInbound(data: ByteArray) {
        if (replayBuffer.size >= replayCapacity) replayBuffer.removeFirst()
        replayBuffer.addLast(data)
        inboundEmitter.emit(data)
    }

    private fun resolveTargetNodes(onResult: (Collection<Node>) -> Unit) {
        Wearable.getCapabilityClient(appContext)
            .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { info ->
                if (info.nodes.isNotEmpty()) onResult(info.nodes)
                else fallbackToConnectedNodes(onResult)
            }
            .addOnFailureListener { fallbackToConnectedNodes(onResult) }
    }

    private fun fallbackToConnectedNodes(onResult: (Collection<Node>) -> Unit) {
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener(onResult)
            .addOnFailureListener { onResult(emptyList()) }
    }

    private fun setState(newState: LinkState) {
        if (state == newState) return
        state = newState
        linkStateEmitter.emit(newState)
    }
}
