package com.dashboard.core.domain

/** Turn-by-turn direction glyph the Maps panel should render. Kept coarse — the phone owns real routing. */
enum class Direction { STRAIGHT, TURN_LEFT, TURN_RIGHT, KEEP_LEFT, KEEP_RIGHT, ROUNDABOUT, ARRIVED, UNKNOWN }

/**
 * What the Maps panel shows. `active = false` means "no navigation running on the phone" —
 * the panel must render that explicitly rather than stale/last-known data.
 *
 * This model is deliberately thin: destination search and route calculation stay on the phone.
 * The dashboard only renders what it's told.
 */
data class NavigationState(
    val active: Boolean = false,
    val direction: Direction = Direction.UNKNOWN,
    val distanceMeters: Double? = null,
    val roadName: String? = null,
    val etaMinutes: Int? = null,
) {
    companion object {
        val INACTIVE = NavigationState(active = false)
    }
}
