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
 * Keyword coverage was expanded 2026-09-07 to the phrasings Google Maps and Waze are documented
 * to emit (English + Croatian; slight and sharp turns collapse to the KEEP and TURN values,
 * ramps to the EXIT values). Still: only add a phrase once it is known to be real — do not
 * invent translations.
 */
object NavigationAnnouncementParser {

    data class Checkpoint(
        val direction: Direction,
        val distanceMeters: Double?,
        val roadName: String?,
    )

    private data class DirectionKeyword(val pattern: Regex, val direction: Direction)

    // Order matters: the FIRST pattern that matches wins, so the more specific maneuvers
    // (arrival, roundabout, u-turn, ramp/exit, merge) are listed before the plain turns.
    private val directionKeywords: List<DirectionKeyword> = listOf(
        kw(Direction.ARRIVED, """\barrived\b""", """\byou have (?:arrived|reached)\b""",
            """\barriving\b""", """\byour destination is\b""", """\bdestination (?:is )?on (?:the |your )?(?:left|right)\b""",
            """stigli ste""", """odredište"""),
        kw(Direction.ROUNDABOUT, """\broundabout\b""", """\btraffic circle\b""", """\brotary\b""",
            """kružn(?:om|i) tok""", """\bkružni tok\b"""),
        kw(Direction.U_TURN, """\bmake a u-?turn\b""", """\bu-?turn\b""", """polukružno"""),
        kw(Direction.EXIT_RIGHT, """\b(?:take|keep right (?:to take|for)) (?:the )?exit\b.*\bright\b""",
            """\bexit (?:on |to )?(?:the )?right\b""", """\btake the ramp on the right\b""",
            """\bkeep right (?:to take|and take|for) (?:the )?exit\b""", """izlaz(?:om)? desno"""),
        kw(Direction.EXIT_LEFT, """\bexit (?:on |to )?(?:the )?left\b""",
            """\btake the ramp on the left\b""", """\bkeep left (?:to take|and take|for) (?:the )?exit\b""",
            """izlaz(?:om)? lijevo"""),
        kw(Direction.MERGE, """\bmerge\b""", """\bmerging\b""", """spojite se"""),
        kw(Direction.KEEP_LEFT, """\bkeep left\b""", """\bbear left\b""", """\bstay left\b""",
            """\bslight(?:ly)? left\b""", """držite se lijeve""", """lijevom trakom"""),
        kw(Direction.KEEP_RIGHT, """\bkeep right\b""", """\bbear right\b""", """\bstay right\b""",
            """\bslight(?:ly)? right\b""", """držite se desne""", """desnom trakom"""),
        kw(Direction.TURN_LEFT, """\bturn left\b""", """\bleft turn\b""", """\bsharp left\b""",
            """\bturn to the left\b""", """\bleft onto\b""", """skrenite (?:ulijevo|lijevo)""", """skreni lijevo"""),
        kw(Direction.TURN_RIGHT, """\bturn right\b""", """\bright turn\b""", """\bsharp right\b""",
            """\bturn to the right\b""", """\bright onto\b""", """skrenite (?:udesno|desno)""", """skreni desno"""),
        kw(Direction.STRAIGHT, """\bcontinue straight\b""", """\bgo straight\b""", """\bhead straight\b""",
            """\bproceed straight\b""", """\bcontinue (?:on|for|onto|to follow)\b""", """\bhead (?:north|south|east|west)\b""",
            """nastavite ravno""", """nastavite (?:cestom|ravno)"""),
    )

    private const val MILE_M = 1609.344
    private const val FOOT_M = 0.3048
    private const val YARD_M = 0.9144

    private val distanceUnit =
        """m|meter|meters|metre|metres|km|kilometer|kilometers|kilometre|kilometres|mi|mile|miles|ft|feet|foot|yd|yard|yards"""

    private val distancePattern = Regex("""(\d+(?:[.,]\d+)?)\s*($distanceUnit)\b""", RegexOption.IGNORE_CASE)

    private val liveBannerFragmentPattern = Regex(
        """^\s*(\d+(?:[.,]\d+)?)\s*($distanceUnit)\s*,\s*(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    private val bareDistanceFragmentPattern = Regex(
        """^\s*\d+(?:[.,]\d+)?\s*($distanceUnit)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val ontoRoadPattern = Regex(
        """\b(?:on(?:to)?|toward|towards|u|na|prema)\s+([A-ZČĆŽŠĐ][\wČĆŽŠĐčćžšđ.'-]*(?:\s+[A-ZČĆŽŠĐ0-9][\wČĆŽŠĐčćžšđ.'-]*)*)""",
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
            val distanceMeters = toMeters(match.groupValues[1], match.groupValues[2])
            val instructionText = match.groupValues[3]
            val direction = findDirection(instructionText) ?: continue

            val roadName = extractRoadName(instructionText)
                ?: fragments.drop(index + 1).firstOrNull {
                    it.isNotBlank() && !bareDistanceFragmentPattern.matches(it) && it.length < 60
                }

            return Checkpoint(direction = direction, distanceMeters = distanceMeters, roadName = roadName)
        }
        return null
    }

    private fun parseSingleSentence(text: String): Checkpoint? {
        val direction = findDirection(text) ?: return null
        val distanceMeters = distancePattern.find(text)
            ?.let { toMeters(it.groupValues[1], it.groupValues[2]) }
        return Checkpoint(direction = direction, distanceMeters = distanceMeters, roadName = extractRoadName(text))
    }

    private fun extractRoadName(text: String): String? =
        ontoRoadPattern.find(text)?.groupValues?.get(1)?.trim()?.ifEmpty { null }

    private fun findDirection(text: String): Direction? =
        directionKeywords.firstOrNull { it.pattern.containsMatchIn(text) }?.direction

    private fun toMeters(rawValue: String, unit: String): Double? {
        val value = rawValue.replace(',', '.').toDoubleOrNull() ?: return null
        return when (unit.lowercase()) {
            "km", "kilometer", "kilometers", "kilometre", "kilometres" -> value * 1000.0
            "mi", "mile", "miles" -> value * MILE_M
            "ft", "feet", "foot" -> value * FOOT_M
            "yd", "yard", "yards" -> value * YARD_M
            else -> value // m and its spellings
        }
    }

    private fun kw(direction: Direction, vararg patterns: String) = DirectionKeyword(
        Regex(patterns.joinToString("|"), RegexOption.IGNORE_CASE),
        direction,
    )
}
