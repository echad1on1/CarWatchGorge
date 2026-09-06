package com.dashboard.core.domain

/**
 * Distance-based urgency tier for a Blizzer camera-proximity event — blue/far, green/approaching,
 * red/close, matching the product spec's color scheme. Kept in `core` (not the UI layer) so the
 * threshold logic is unit-testable without a device; the UI layer only maps each tier to an
 * actual color.
 */
enum class BlizzerSeverity { INFO, DISTANT, APPROACHING, CLOSE }

object BlizzerSeverityMapper {
    /** null distance (a non-proximity Blizzer event) always maps to INFO (neutral). */
    fun forDistance(distanceMeters: Int?): BlizzerSeverity = when {
        distanceMeters == null -> BlizzerSeverity.INFO
        distanceMeters > 1000 -> BlizzerSeverity.DISTANT
        distanceMeters > 500 -> BlizzerSeverity.APPROACHING
        else -> BlizzerSeverity.CLOSE
    }
}
