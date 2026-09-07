package com.dashboard.phoneapp

import android.util.Log
import com.dashboard.core.communication.MessageCodec
import com.dashboard.core.communication.ProtocolMessage
import com.dashboard.core.communication.toDomain
import com.dashboard.phoneapp.media.MediaSessionHub
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives frames the watch sends back (currently only `MediaCommandMessage` — the ⏮ ⏯ ⏭
 * buttons on the Music panel) and applies them to the phone's active media session via
 * [MediaSessionHub]. Registered for the same Data Layer path prefix as everything else; the
 * `MessageCodec` type tag on line 1 says what each frame is.
 */
class WearInboundListenerService : WearableListenerService() {

    private companion object {
        const val TAG = "WearInbound"
        const val PATH_PREFIX = "/automotive-dashboard"
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (!event.path.startsWith(PATH_PREFIX)) return
        val message = runCatching { MessageCodec.decode(event.data) }.getOrNull() ?: return
        when (message) {
            is ProtocolMessage.MediaCommandMessage -> {
                val command = runCatching { message.toDomain() }.getOrNull() ?: return
                Log.d(TAG, "media command from watch: $command")
                MediaSessionHub.dispatch(command)
            }
            else -> Unit // nav/media/blizzer state frames are watch-bound, ignore here
        }
    }
}
