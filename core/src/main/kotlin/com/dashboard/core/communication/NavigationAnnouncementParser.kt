package com.dashboard.core.communication

import com.dashboard.core.domain.Direction

/**
 * Parses the spoken text of a turn-by-turn navigation announcement (e.g. "In 200 meters, turn
 * left", "Turn right onto Main Street", "You have arrived") into a structured checkpoint.
 *
 * ## Why this exists
 * There is no official, generic Android API for reading structured navigation state from an
 * arbitrary app the user has running (see docs/android-integration-research.md). What Google
 * Maps/Waze DO reliably provide, for accessibility purposes, is spoken/announced text — the same
 * text a screen reader would read aloud. A phone-side `AccessibilityService` can capture that
 * text (see the `phone-app` module); this parser is what turns it into something
 * [com.dashboard.core.domain.NavigationState] can use.
 *
 * ## What this gives you vs. doesn't
 * An announcement is a discrete checkpoint ("next turn is 200m away *right now*"), not a
 * continuous feed. [com.dashboard.core.service.NavigationManager] is responsible for smoothly
 * counting that distance down between announcements using the vehicle's own live speed — this
 * parser's only job is turning one sentence into one checkpoint.
 *
 * ## Reliability
 * This is inherently fragile — it depends on the exact phrasing a third-party app happens to use
 * for its accessibility announcements, which that app can change at any time without notice. The
 * patterns below were written to be permissive (multiple phrasings, case-insensitive, tolerant of
 * extra words) precisely because of that fragility, but this should be expected to need
 * maintenance as real-world announcement text is observed from actual apps.
 */
object NavigationAnnouncementParser {

    data class Checkpoint(
        val direction: Direction,
        val distanceMeters: Double?,
        val roadName: String?,
    )

    private val distancePattern = Regex(
        """\b(\d+(?:\.\d+)?)\s*(m|meter|meters|metre|metres|km|kilometer|kilometers|kilometre|kilometres)\b""",
        RegexOption.IGNORE_CASE,
    )

    // "onto <Road Name>" / "on <Road Name>" / "on to <Road Name>" — captures the road name, if present.
    private val ontoRoadPattern = Regex(
        """\bon(?:to)?\s+([A-Z][\w\s.'-]*?)(?:[.,]|$)""",
        setOf(RegexOption.IGNORE_CASE),
    )

    /** Returns null if [text] doesn't look like a navigation announcement at all. */
    fun parse(text: String): Checkpoint? {
        val normalized = text.trim()
        if (normalized.isEmpty()) return null

        val direction = detectDirection(normalized) ?: return null
        val distanceMeters = detectDistanceMeters(normalized)
        val roadName = detectRoadName(normalized)

        return Checkpoint(direction = direction, distanceMeters = distanceMeters, roadName = roadName)
    }

    private fun detectDirection(text: String): Direction? {
        val lower = text.lowercase()
        return when {
            "arrived" in lower || "you have reached" in lower -> Direction.ARRIVED
            "roundabout" in lower || "traffic circle" in lower -> Direction.ROUNDABOUT
            "keep left" in lower || "bear left" in lower || "stay left" in lower -> Direction.KEEP_LEFT
            "keep right" in lower || "bear right" in lower || "stay right" in lower -> Direction.KEEP_RIGHT
            "turn left" in lower || "left turn" in lower -> Direction.TURN_LEFT
            "turn right" in lower || "right turn" in lower -> Direction.TURN_RIGHT
            "continue straight" in lower || "go straight" in lower || "head straight" in lower -> Direction.STRAIGHT
            else -> null
        }
    }

    private fun detectDistanceMeters(text: String): Double? {
        val match = distancePattern.find(text) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].lowercase()
        return if (unit.startsWith("k")) value * 1000.0 else value
    }

    private fun detectRoadName(text: String): String? {
        val match = ontoRoadPattern.find(text) ?: return null
        val name = match.groupValues[1].trim()
        return name.ifEmpty { null }
    }
}
