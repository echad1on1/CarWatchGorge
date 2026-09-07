package com.dashboard.phoneapp.media

import android.media.session.MediaController
import com.dashboard.core.domain.MediaCommand

/**
 * Process-wide handle to the media session the dashboard is currently mirroring, so
 * [com.dashboard.phoneapp.WearInboundListenerService] can act on a `MediaCommandMessage` from
 * the watch without itself binding to `MediaSessionManager`. Set by
 * [MediaNotificationListenerService]; null when nothing is playable.
 */
object MediaSessionHub {

    @Volatile
    var current: MediaController? = null

    fun dispatch(command: MediaCommand) {
        val controls = current?.transportControls ?: return
        when (command) {
            MediaCommand.PLAY -> controls.play()
            MediaCommand.PAUSE -> controls.pause()
            MediaCommand.NEXT -> controls.skipToNext()
            MediaCommand.PREVIOUS -> controls.skipToPrevious()
        }
    }
}
