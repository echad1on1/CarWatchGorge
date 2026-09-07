package com.dashboard.core.communication

import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.PlaybackState

/**
 * A minimal, Android-free snapshot of one phone-side media session. The phone companion builds
 * one of these per active `MediaController` and hands the list to [MediaSessionSelection] so the
 * "which app do we follow" rule stays in `core` and is unit-testable without a device.
 */
data class SessionSnapshot(
    val packageName: String,
    val state: MediaState,
    /** When this session was last the active/most-recent one, from the phone's clock. */
    val lastActiveMillis: Long,
)

/**
 * Decides which media session the Music panel mirrors when several apps hold a session at once
 * (e.g. a paused podcast and a playing radio). Pure function — no Android, no side effects.
 */
object MediaSessionSelection {

    /**
     * Rules, in order:
     *  1. a `PLAYING` session beats any non-playing one;
     *  2. then a `PAUSED` session beats `STOPPED`/`UNKNOWN` (something half-open beats nothing);
     *  3. ties broken by most-recently-active;
     *  4. `null` only when there are no sessions at all.
     */
    fun chooseActive(candidates: List<SessionSnapshot>): SessionSnapshot? {
        if (candidates.isEmpty()) return null
        return candidates.maxWith(
            compareBy<SessionSnapshot> { rank(it.state.playbackState) }
                .thenBy { it.lastActiveMillis }
        )
    }

    private fun rank(state: PlaybackState): Int = when (state) {
        PlaybackState.PLAYING -> 3
        PlaybackState.PAUSED -> 2
        PlaybackState.STOPPED -> 1
        PlaybackState.UNKNOWN -> 0
    }
}
