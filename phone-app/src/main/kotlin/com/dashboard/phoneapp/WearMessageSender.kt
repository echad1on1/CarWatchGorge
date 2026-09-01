package com.dashboard.phoneapp

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable

/**
 * Sends encoded nav checkpoints from the phone companion app to the paired watch via the
 * Wear OS Data Layer [com.google.android.gms.wearable.MessageClient].
 */
object WearMessageSender {

  private const val TAG = "WearMessageSender"
  const val NAV_MESSAGE_PATH = "/automotive-dashboard/nav"

  fun sendNavUpdate(context: Context, data: ByteArray) {
    val appContext = context.applicationContext
    val nodeClient = Wearable.getNodeClient(appContext)
    val messageClient = Wearable.getMessageClient(appContext)

    nodeClient.connectedNodes
      .addOnSuccessListener { nodes ->
        if (nodes.isEmpty()) {
          Log.w(TAG, "No connected watch nodes — nav update not sent (${data.size} bytes)")
          return@addOnSuccessListener
        }
        for (node in nodes) {
          messageClient
            .sendMessage(node.id, NAV_MESSAGE_PATH, data)
            .addOnSuccessListener {
              Log.d(TAG, "Sent ${data.size} bytes to watch node ${node.displayName}")
            }
            .addOnFailureListener { error ->
              Log.e(TAG, "Failed to send nav update to ${node.displayName}", error)
            }
        }
      }
      .addOnFailureListener { error ->
        Log.e(TAG, "Failed to resolve connected watch nodes", error)
      }
  }
}
