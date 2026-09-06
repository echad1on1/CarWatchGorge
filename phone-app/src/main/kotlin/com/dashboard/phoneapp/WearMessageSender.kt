package com.dashboard.phoneapp

import android.content.Context
import com.google.android.gms.wearable.Wearable

private const val MESSAGE_PATH = "/automotive-dashboard/nav"

/** Sends encoded navigation checkpoints to any connected Wear OS watch. */
object WearMessageSender {
    fun sendNavUpdate(context: Context, data: ByteArray) {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, MESSAGE_PATH, data)
            }
        }
    }
}
