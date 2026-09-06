package com.dashboard.core.domain

/**
 * Maps camera-proximity distance to visual urgency. Pure functions only — no UI or audio deps.
 * Thresholds align with the real Blizzer app's beep cadence: 2000/1000/500/200/100 m.
 */
object BlizzerProximity {
    const val COLOR_BLUE: Long = 0xFF2196F3
    const val COLOR_GREEN: Long = 0xFF4CAF50
    const val COLOR_AMBER: Long = 0xFFFF9800
    const val COLOR_RED: Long = 0xFFB00020
    const val COLOR_NEUTRAL: Long = 0xFF1A1A2E

    /** ARGB color for the overlay background. Null distance → neutral (non-proximity events). */
    fun colorArgbFor(distanceMeters: Int?): Long = when {
        distanceMeters == null -> COLOR_NEUTRAL
        distanceMeters > 1000 -> COLOR_BLUE
        distanceMeters > 500 -> COLOR_GREEN
        distanceMeters > 200 -> COLOR_AMBER
        else -> COLOR_RED
    }

    /** Closer camera → faster blink. Null distance blinks at a calm default rate. */
    fun blinkPeriodMillisFor(distanceMeters: Int?): Int = when {
        distanceMeters == null -> 900
        distanceMeters > 1000 -> 1000
        distanceMeters > 500 -> 800
        distanceMeters > 200 -> 650
        distanceMeters > 100 -> 400
        else -> 220
    }
}
