package com.dashboard.core.hardware.mock

import com.dashboard.core.communication.MessageCodec
import com.dashboard.core.communication.ProtocolMessage
import com.dashboard.core.communication.toDomain
import com.dashboard.core.communication.toProtocol
import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.BlizzerEventType
import com.dashboard.core.domain.Direction
import com.dashboard.core.domain.MediaCommand
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.NavigationState
import com.dashboard.core.domain.PlaybackState
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.PhoneCommunication
import com.dashboard.core.hardware.Subscription

/**
 * Developer-facing stand-in for a connected phone. Provides the "Start Navigation", "Change
 * Direction", "Start Music", "Next Song", "Trigger Blizzer", etc. controls described in the
 * spec's developer testing interface.
 *
 * Every dev-control method here builds the same [ProtocolMessage] a real phone integration would
 * send, round-trips it through [MessageCodec] (encode then decode), and only then updates its
 * internal state and notifies listeners. This means the codec is exercised on every single dev
 * action, not just in dedicated codec tests — a codec bug would break the demo immediately, not
 * silently pass because the mock bypassed it.
 */
class MockPhoneCommunication : PhoneCommunication {

    private val navigationEmitter = Emitter<NavigationState>()
    private val mediaEmitter = Emitter<MediaState>()
    private val blizzerEmitter = Emitter<BlizzerEvent>()

    var currentNavigation: NavigationState = NavigationState.INACTIVE
        private set
    var currentMedia: MediaState = MediaState.NONE
        private set

    private val songLibrary = listOf(
        MediaState(title = "Night Drive", artist = "Kepler Freeway", album = "Ignition", durationMillis = 214_000),
        MediaState(title = "Overpass", artist = "Kepler Freeway", album = "Ignition", durationMillis = 198_000),
        MediaState(title = "Low Beam", artist = "Kepler Freeway", album = "Ignition", durationMillis = 231_000),
    )
    private var songIndex = 0
    private var currentBlizzerId: String? = null

    override fun observeNavigationState(listener: (NavigationState) -> Unit): Subscription {
        listener(currentNavigation)
        return navigationEmitter.subscribe(listener)
    }

    override fun observeMediaState(listener: (MediaState) -> Unit): Subscription {
        listener(currentMedia)
        return mediaEmitter.subscribe(listener)
    }

    override fun observeBlizzerEvents(listener: (BlizzerEvent) -> Unit): Subscription =
        blizzerEmitter.subscribe(listener)

    override fun sendMediaCommand(command: MediaCommand) {
        // Round-trip even outbound commands, exactly as they'd be encoded for real transport.
        val decoded = roundTrip(command.toProtocol()) as ProtocolMessage.MediaCommandMessage
        when (decoded.toDomain()) {
            MediaCommand.PLAY -> setMedia(currentMedia.copy(playbackState = PlaybackState.PLAYING))
            MediaCommand.PAUSE -> setMedia(currentMedia.copy(playbackState = PlaybackState.PAUSED))
            MediaCommand.NEXT -> nextSong()
            MediaCommand.PREVIOUS -> previousSong()
        }
    }

    // ---- Developer controls -------------------------------------------------------------

    fun startNavigation(roadName: String = "Ridge Valley Rd", etaMinutes: Int = 12) {
        setNavigation(
            NavigationState(
                active = true,
                direction = Direction.STRAIGHT,
                distanceMeters = 800.0,
                roadName = roadName,
                etaMinutes = etaMinutes,
            )
        )
    }

    fun stopNavigation() {
        setNavigation(NavigationState.INACTIVE)
    }

    /**
     * The real-world entry point this is all built for: feed it raw spoken announcement text
     * (what a phone-side AccessibilityService would capture from Google Maps/Waze — see
     * docs/android-integration-research.md and [com.dashboard.core.communication.NavigationAnnouncementParser])
     * and it updates navigation state exactly the way a real announcement would. Distance/road
     * name fall back to whatever's already known if this particular sentence doesn't mention them
     * (e.g. "Turn left" alone, with the distance from an earlier "In 200 meters..." still standing).
     *
     * Returns false if [rawText] doesn't parse as a navigation announcement at all (no-op).
     */
    fun announceNavigation(rawText: String): Boolean {
        val checkpoint = com.dashboard.core.communication.NavigationAnnouncementParser.parse(rawText) ?: return false
        val isArrival = checkpoint.direction == Direction.ARRIVED
        setNavigation(
            currentNavigation.copy(
                active = true,
                direction = checkpoint.direction,
                distanceMeters = when {
                    isArrival -> 0.0 // "arrived" shouldn't keep showing a stale prior distance
                    else -> checkpoint.distanceMeters ?: currentNavigation.distanceMeters
                },
                roadName = checkpoint.roadName ?: currentNavigation.roadName,
            )
        )
        return true
    }

    fun changeDirection(direction: Direction, distanceMeters: Double, roadName: String? = currentNavigation.roadName) {
        if (!currentNavigation.active) return
        setNavigation(currentNavigation.copy(direction = direction, distanceMeters = distanceMeters, roadName = roadName))
    }

    fun decreaseDistance(byMeters: Double) {
        if (!currentNavigation.active) return
        val remaining = (currentNavigation.distanceMeters ?: 0.0) - byMeters
        setNavigation(currentNavigation.copy(distanceMeters = remaining.coerceAtLeast(0.0)))
    }

    fun startMusic() {
        val song = songLibrary[songIndex]
        setMedia(song.copy(playbackState = PlaybackState.PLAYING))
    }

    fun pauseMusic() {
        setMedia(currentMedia.copy(playbackState = PlaybackState.PAUSED))
    }

    fun nextSong() {
        songIndex = (songIndex + 1) % songLibrary.size
        setMedia(songLibrary[songIndex].copy(playbackState = PlaybackState.PLAYING))
    }

    fun previousSong() {
        songIndex = (songIndex - 1 + songLibrary.size) % songLibrary.size
        setMedia(songLibrary[songIndex].copy(playbackState = PlaybackState.PLAYING))
    }

    fun triggerBlizzer(message: String, type: BlizzerEventType = BlizzerEventType.INFO): String {
        val id = "blizzer-${System.currentTimeMillis()}"
        val event = BlizzerEvent(
            id = id,
            type = type,
            message = message,
            timestampMillis = System.currentTimeMillis(),
            active = true,
        )
        val decoded = roundTrip(event.toProtocol()) as ProtocolMessage.BlizzerTrigger
        blizzerEmitter.emit(decoded.toDomain())
        return id
    }

    /**
     * Developer control matching how the real Blizzer app behaves: a camera-proximity beep/alert
     * at a given distance (e.g. 500, 200, 100 meters — closer = more urgent). Reuses [currentBlizzerId]
     * so successive calls at shrinking distances update the SAME event rather than stacking new ones,
     * matching one continuous approach-to-camera experience rather than three separate popups.
     */
    fun triggerCameraWarning(distanceMeters: Int): String {
        val id = currentBlizzerId ?: "blizzer-camera-${System.currentTimeMillis()}"
        currentBlizzerId = id
        val type = if (distanceMeters <= 150) BlizzerEventType.ALERT else BlizzerEventType.WARNING
        val event = BlizzerEvent(
            id = id,
            type = type,
            message = "Speed camera in ${distanceMeters}m",
            timestampMillis = System.currentTimeMillis(),
            active = true,
            distanceMeters = distanceMeters,
        )
        val decoded = roundTrip(event.toProtocol()) as ProtocolMessage.BlizzerTrigger
        blizzerEmitter.emit(decoded.toDomain())
        return id
    }

    /** Developer control: signal that a Blizzer event has finished, mirroring stopNavigation(). */
    fun dismissBlizzer(id: String, message: String = "") {
        if (currentBlizzerId == id) currentBlizzerId = null
        val event = BlizzerEvent(
            id = id,
            type = BlizzerEventType.INFO,
            message = message,
            timestampMillis = System.currentTimeMillis(),
            active = false,
        )
        val decoded = roundTrip(event.toProtocol()) as ProtocolMessage.BlizzerTrigger
        blizzerEmitter.emit(decoded.toDomain())
    }

    /** Dismisses whatever camera-proximity event is currently active, if any — no id needed. */
    fun dismissCameraWarning() {
        currentBlizzerId?.let { dismissBlizzer(it) }
    }

    // ---- Internals ------------------------------------------------------------------------

    private fun setNavigation(state: NavigationState) {
        val decoded = roundTrip(state.toProtocol()) as ProtocolMessage.NavigationUpdate
        currentNavigation = decoded.toDomain()
        navigationEmitter.emit(currentNavigation)
    }

    private fun setMedia(state: MediaState) {
        val decoded = roundTrip(state.toProtocol()) as ProtocolMessage.MediaUpdate
        currentMedia = decoded.toDomain()
        mediaEmitter.emit(currentMedia)
    }

    private fun roundTrip(message: ProtocolMessage): ProtocolMessage =
        MessageCodec.decode(MessageCodec.encode(message))
}
