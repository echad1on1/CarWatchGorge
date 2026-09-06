package com.dashboard.core.communication

import com.dashboard.core.domain.Direction

/**
 * Parses the on-screen turn-by-turn text captured from Google Maps or Waze (see `phone-app`'s
 * `NavigationAccessibilityService`) into a structured checkpoint.
 *
 * Rewritten after testing against REAL live Google Maps navigation on a real device. Two real
 * findings shaped this version:
 *
 * 1. The captured text is a noisy, pipe-joined blob of the entire screen — it contains an
 *    unrelated overall trip-remaining-distance figure alongside the actual turn instruction, and
 *    the trip-total can appear BEFORE the turn-specific distance in the same blob.
 * 2. Google Maps' own compact turn-by-turn widget renders as a distinctive, reliable shape: a
 *    pipe-separated fragment of the exact form "<number> <unit>, <instruction text>" (e.g.
 *    "400 m, Skrenite udesno u Priestershof"). This shape was never observed on the unrelated
 *    trip-total text, which is what makes searching for it specifically reliable.
 *
 * Only phrases actually observed in captured data are included (English from initial design;
 * Croatian confirmed via real device testing) — extend as more real text is observed, rather
 * than guessing translations.
 */
object NavigationAnnouncementParser {

    data class Checkpoint(
        val direction: Direction,
        val distanceMeters: Double?,
        val roadName: String?,
    )

    private data class DirectionKeyword(val pattern: Regex, val direction: Direction)

    private val directionKeywords: List<DirectionKeyword> = listOf(
        DirectionKeyword(Regex("""\barrived\b|\byou have reached\b""", RegexOption.IGNORE_CASE), Direction.ARRIVED),
        DirectionKeyword(Regex("""\broundabout\b|\btraffic circle\b""", RegexOption.IGNORE_CASE), Direction.ROUNDABOUT),
        DirectionKeyword(Regex("""\bkeep left\b|\bbear left\b|\bstay left\b""", RegexOption.IGNORE_CASE), Direction.KEEP_LEFT),
        DirectionKeyword(Regex("""\bkeep right\b|\bbear right\b|\bstay right\b""", RegexOption.IGNORE_CASE), Direction.KEEP_RIGHT),
        DirectionKeyword(Regex("""\bturn left\b|\bleft turn\b""", RegexOption.IGNORE_CASE), Direction.TURN_LEFT),
        DirectionKeyword(Regex("""\bturn right\b|\bright turn\b""", RegexOption.IGNORE_CASE), Direction.TURN_RIGHT),
        DirectionKeyword(Regex("""\bcontinue straight\b|\bgo straight\b|\bhead straight\b""", RegexOption.IGNORE_CASE), Direction.STRAIGHT),
        DirectionKeyword(Regex("""kružnom toku|kružni tok""", RegexOption.IGNORE_CASE), Direction.ROUNDABOUT),
        DirectionKeyword(Regex("""skrenite ulijevo""", RegexOption.IGNORE_CASE), Direction.TURN_LEFT),
        DirectionKeyword(Regex("""skrenite udesno""", RegexOption.IGNORE_CASE), Direction.TURN_RIGHT),
    )

    private val distancePattern = Regex(
        """(\d+(?:\.\d+)?)\s*(m|meter|meters|metre|metres|km|kilometer|kilometers|kilometre|kilometres)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val liveBannerFragmentPattern = Regex(
        """^\s*(\d+(?:\.\d+)?)\s*(m|meter|meters|metre|metres|km|kilometer|kilometers|kilometre|kilometres)\s*,\s*(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    private val bareDistanceFragmentPattern = Regex(
        """^\s*\d+(?:\.\d+)?\s*(m|km|meter|meters|kilometer|kilometers)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val ontoRoadPattern = Regex(
        """\b(?:on(?:to)?|u)\s+([A-ZČĆŽŠĐ][\wČĆŽŠĐčćžšđ.'-]*(?:\s+[A-ZČĆŽŠĐ][\wČĆŽŠĐčćžšđ.'-]*)*)""",
    )

    fun parse(text: String): Checkpoint? {
        val normalized = text.trim()
        if (normalized.isEmpty()) return null

        parseFromFragments(normalized.split("|").map { it.trim() })?.let { return it }
        return parseSingleSentence(normalized)
    }

    private fun parseFromFragments(fragments: List<String>): Checkpoint? {
        for ((index, fragment) in fragments.withIndex()) {
            val match = liveBannerFragmentPattern.find(fragment) ?: continue
            val distanceMeters = toMeters(match.groupValues[1].toDoubleOrNull(), match.groupValues[2])
            val instructionText = match.groupValues[3]
            val direction = findDirection(instructionText) ?: continue

            val roadName = fragments.drop(index + 1)
                .firstOrNull { it.isNotBlank() && !bareDistanceFragmentPattern.matches(it) && it.length < 60 }

            return Checkpoint(direction = direction, distanceMeters = distanceMeters, roadName = roadName)
        }
        return null
    }

    private fun parseSingleSentence(text: String): Checkpoint? {
        val direction = findDirection(text) ?: return null
        val distanceMeters = distancePattern.find(text)
            ?.let { toMeters(it.groupValues[1].toDoubleOrNull(), it.groupValues[2]) }
        val roadName = ontoRoadPattern.find(text)?.groupValues?.get(1)?.trim()?.ifEmpty { null }
        return Checkpoint(direction = direction, distanceMeters = distanceMeters, roadName = roadName)
    }

    private fun findDirection(text: String): Direction? =
        directionKeywords.firstOrNull { it.pattern.containsMatchIn(text) }?.direction

    private fun toMeters(value: Double?, unit: String): Double? {
        if (value == null) return null
        return if (unit.lowercase().startsWith("k")) value * 1000.0 else value
    }
}
