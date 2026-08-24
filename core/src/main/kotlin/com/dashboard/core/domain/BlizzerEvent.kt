package com.dashboard.core.domain

/** Category of Blizzer interruption. Kept as an open-ish enum; extend as real Blizzer events are defined. */
enum class BlizzerEventType { INFO, WARNING, ANIMATION, ALERT }

/**
 * A single Blizzer interruption. Blizzer is a speed-camera/road-hazard proximity alert (like
 * the real "Blitzer" phone app it's modeled on) — it's a global overlay that can appear above
 * Car, Maps or Music and must return the user to whatever they were viewing once dismissed.
 *
 * The real app beeps as the driver approaches a known camera, typically at decreasing distance
 * thresholds (e.g. 500m, 200m, 100m). [distanceMeters] carries that thresholds so the UI can
 * increase visual urgency (faster blinking) as the distance shrinks, and so an audio manager can
 * fire a beep on each new threshold — see [com.dashboard.core.service.BlizzerAudioManager].
 * `null` means this event isn't distance-based (a generic info/animation event).
 */
data class BlizzerEvent(
    val id: String,
    val type: BlizzerEventType,
    val message: String,
    val timestampMillis: Long,
    val active: Boolean = true,
    val distanceMeters: Int? = null,
)
