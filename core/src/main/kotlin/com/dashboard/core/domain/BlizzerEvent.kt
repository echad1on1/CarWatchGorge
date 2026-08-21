package com.dashboard.core.domain

/** Category of Blizzer interruption. Kept as an open-ish enum; extend as real Blizzer events are defined. */
enum class BlizzerEventType { INFO, WARNING, ANIMATION, ALERT }

/**
 * A single Blizzer interruption. Blizzer is not a panel — it's a global overlay that can
 * appear above Car, Maps or Music and must return the user to whatever they were viewing.
 */
data class BlizzerEvent(
    val id: String,
    val type: BlizzerEventType,
    val message: String,
    val timestampMillis: Long,
    val active: Boolean = true,
)
