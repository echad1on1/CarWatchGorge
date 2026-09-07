package com.dashboard.phoneapp

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

/**
 * Sends encoded frames to the paired dashboard watch over the Wear OS Data Layer.
 *
 * All payloads go on one path ([MESSAGE_PATH]); the watch's `BluetoothPhoneCommunication`
 * demultiplexes by the type tag `MessageCodec` writes on line 1, so nav / media / blizzer
 * frames all use [send]. `sendNavUpdate` is kept as a named alias because a past cross-session
 * refactor renamed it and broke the caller — do not remove it.
 */
object WearMessageSender {

    /** Single Data Layer path for every dashboard frame; the codec tag says what each one is. */
    const val MESSAGE_PATH = "/automotive-dashboard/nav"

    /** Capability the watch node advertises (see wearos-app `res/values/wear.xml`). */
    private const val WATCH_CAPABILITY = "automotive_dashboard_watch"

    fun send(context: Context, data: ByteArray) {
        val appContext = context.applicationContext
        resolveWatchNodes(appContext) { nodes ->
            val client = Wearable.getMessageClient(appContext)
            nodes.forEach { node -> client.sendMessage(node.id, MESSAGE_PATH, data) }
        }
    }

    /** Named alias for [send] — kept for source compatibility with existing callers. */
    fun sendNavUpdate(context: Context, data: ByteArray) = send(context, data)

    private fun resolveWatchNodes(context: Context, onResult: (Collection<Node>) -> Unit) {
        Wearable.getCapabilityClient(context)
            .getCapability(WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { info ->
                if (info.nodes.isNotEmpty()) onResult(info.nodes)
                else fallback(context, onResult)
            }
            .addOnFailureListener { fallback(context, onResult) }
    }

    private fun fallback(context: Context, onResult: (Collection<Node>) -> Unit) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener(onResult)
            .addOnFailureListener { onResult(emptyList()) }
    }
}
