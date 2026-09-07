package com.dashboard.phoneapp.media

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState as AndroidPlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import com.dashboard.core.communication.MediaSessionSelection
import com.dashboard.core.communication.SessionSnapshot
import com.dashboard.core.communication.toProtocol
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.PlaybackState
import com.dashboard.core.communication.MessageCodec
import com.dashboard.phoneapp.WearMessageSender

/**
 * Reads whatever media session is active on the phone (Spotify, YouTube Music, a podcast app, …)
 * via [MediaSessionManager] and forwards a [MediaState] to the watch. Needs the user to grant
 * "Notification access" once — that is what makes `getActiveSessions` return other apps' sessions
 * without each app's cooperation (see docs/android-integration-research.md).
 *
 * This service does nothing with notifications themselves; `NotificationListenerService` is only
 * the vehicle for the notification-access grant that `MediaSessionManager` requires.
 */
class MediaNotificationListenerService : NotificationListenerService() {

    private companion object {
        const val TAG = "MediaSessionReader"
    }

    private val sessionManager: MediaSessionManager by lazy {
        getSystemService(MediaSessionManager::class.java)
    }
    private val componentName by lazy {
        ComponentName(this, MediaNotificationListenerService::class.java)
    }

    private var trackedControllers: List<MediaController> = emptyList()
    private var lastSentState: MediaState? = null
    private val lastActiveMillis = HashMap<String, Long>()

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            rebindControllers(controllers ?: emptyList())
            publish()
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: AndroidPlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = publish()
    }

    override fun onListenerConnected() {
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            rebindControllers(sessionManager.getActiveSessions(componentName))
            publish()
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification access not granted yet: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        rebindControllers(emptyList())
    }

    private fun rebindControllers(controllers: List<MediaController>) {
        trackedControllers.forEach { runCatching { it.unregisterCallback(controllerCallback) } }
        trackedControllers = controllers
        val now = System.currentTimeMillis()
        controllers.forEach { c ->
            c.registerCallback(controllerCallback)
            if (c.playbackState?.state == AndroidPlaybackState.STATE_PLAYING) {
                lastActiveMillis[c.packageName] = now
            }
            lastActiveMillis.putIfAbsent(c.packageName, now)
        }
    }

    private fun publish() {
        val byPackage = trackedControllers.associateBy { it.packageName }
        val snapshots = trackedControllers.map { c ->
            SessionSnapshot(
                packageName = c.packageName,
                state = c.toMediaState(),
                lastActiveMillis = lastActiveMillis[c.packageName] ?: 0L,
            )
        }
        val chosenSnapshot = MediaSessionSelection.chooseActive(snapshots)
        MediaSessionHub.current = chosenSnapshot?.let { byPackage[it.packageName] }

        val chosen = chosenSnapshot?.state ?: MediaState.NONE
        if (chosen == lastSentState) return
        lastSentState = chosen
        WearMessageSender.send(applicationContext, MessageCodec.encode(chosen.toProtocol()))
    }
}

private fun MediaController.toMediaState(): MediaState {
    val md = metadata
    val pb = playbackState
    return MediaState(
        title = md?.getString(MediaMetadata.METADATA_KEY_TITLE),
        artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
        album = md?.getString(MediaMetadata.METADATA_KEY_ALBUM),
        playbackState = when (pb?.state) {
            AndroidPlaybackState.STATE_PLAYING, AndroidPlaybackState.STATE_BUFFERING -> PlaybackState.PLAYING
            AndroidPlaybackState.STATE_PAUSED -> PlaybackState.PAUSED
            AndroidPlaybackState.STATE_STOPPED, AndroidPlaybackState.STATE_NONE -> PlaybackState.STOPPED
            else -> PlaybackState.UNKNOWN
        },
        positionMillis = pb?.position ?: 0L,
        durationMillis = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
    )
}
