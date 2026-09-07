package com.dashboard.core.tests

import com.dashboard.core.communication.NavigationAnnouncementParser
import com.dashboard.core.domain.Direction
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

fun navigationAnnouncementParserSuite() = TestSuite("NavigationAnnouncementParser").apply {

    test("parses 'In 200 meters, turn left'") {
        val checkpoint = NavigationAnnouncementParser.parse("In 200 meters, turn left")
        assertTrue(checkpoint != null, "should parse")
        assertEquals(Direction.TURN_LEFT, checkpoint!!.direction, "direction")
        assertEquals(200.0, checkpoint.distanceMeters, "distance")
    }

    test("parses 'Turn right onto Main Street'") {
        val checkpoint = NavigationAnnouncementParser.parse("Turn right onto Main Street")
        assertTrue(checkpoint != null, "should parse")
        assertEquals(Direction.TURN_RIGHT, checkpoint!!.direction, "direction")
        assertEquals("Main Street", checkpoint.roadName, "road name")
    }

    test("converts kilometers to meters") {
        val checkpoint = NavigationAnnouncementParser.parse("In 1.5 km, keep left")
        assertTrue(checkpoint != null, "should parse")
        assertEquals(Direction.KEEP_LEFT, checkpoint!!.direction, "direction")
        assertEquals(1500.0, checkpoint.distanceMeters, "1.5km should become 1500m")
    }

    test("parses roundabout instructions") {
        val checkpoint = NavigationAnnouncementParser.parse("At the roundabout, take the second exit")
        assertTrue(checkpoint != null, "should parse")
        assertEquals(Direction.ROUNDABOUT, checkpoint!!.direction, "roundabout wins over the 'exit' keyword")
    }

    test("parses arrival") {
        val checkpoint = NavigationAnnouncementParser.parse("You have arrived at your destination")
        assertTrue(checkpoint != null, "should parse")
        assertEquals(Direction.ARRIVED, checkpoint!!.direction, "direction")
    }

    test("parses 'Continue straight' with no distance or road name") {
        val checkpoint = NavigationAnnouncementParser.parse("Continue straight")
        assertTrue(checkpoint != null, "should parse")
        assertEquals(Direction.STRAIGHT, checkpoint!!.direction, "direction")
        assertEquals(null, checkpoint.distanceMeters, "no distance mentioned")
    }

    test("is case-insensitive") {
        val checkpoint = NavigationAnnouncementParser.parse("TURN LEFT IN 50 METERS")
        assertTrue(checkpoint != null, "should parse regardless of case")
        assertEquals(Direction.TURN_LEFT, checkpoint!!.direction, "direction")
        assertEquals(50.0, checkpoint.distanceMeters, "distance")
    }

    test("returns null for text that isn't a navigation announcement") {
        val checkpoint = NavigationAnnouncementParser.parse("Welcome to Kepler Freeway radio")
        assertEquals(null, checkpoint, "unrelated text should not parse as a checkpoint")
    }

    test("returns null for empty text") {
        val checkpoint = NavigationAnnouncementParser.parse("   ")
        assertEquals(null, checkpoint, "blank text should not parse")
    }

    // --- expanded 2026-09-07: more real Maps/Waze phrasings ------------------------------

    test("parses the Google Maps live-banner fragment shape with a Croatian instruction") {
        val blob = "1.2 km remaining | 400 m, Skrenite udesno u Priestershof | ETA 12:04"
        val checkpoint = NavigationAnnouncementParser.parse(blob)
        assertTrue(checkpoint != null, "should parse the '<n> <unit>, <instruction>' fragment")
        assertEquals(Direction.TURN_RIGHT, checkpoint!!.direction, "skrenite udesno = turn right")
        assertEquals(400.0, checkpoint.distanceMeters, "uses the turn-specific 400 m, not the 1.2 km trip total")
        assertEquals("Priestershof", checkpoint.roadName, "road from 'u <Name>'")
    }

    test("parses a U-turn") {
        val checkpoint = NavigationAnnouncementParser.parse("In 100 m, make a U-turn")
        assertEquals(Direction.U_TURN, checkpoint?.direction, "u-turn")
        assertEquals(100.0, checkpoint?.distanceMeters, "distance")
    }

    test("parses merge") {
        val checkpoint = NavigationAnnouncementParser.parse("Merge onto I-90 West")
        assertEquals(Direction.MERGE, checkpoint?.direction, "merge")
    }

    test("ramp/exit maps to EXIT_RIGHT") {
        val checkpoint = NavigationAnnouncementParser.parse("In 500 m, take the exit on the right")
        assertEquals(Direction.EXIT_RIGHT, checkpoint?.direction, "exit on the right")
        assertEquals(500.0, checkpoint?.distanceMeters, "distance")
    }

    test("slight left collapses to KEEP_LEFT") {
        val checkpoint = NavigationAnnouncementParser.parse("Slight left onto Elm Avenue")
        assertEquals(Direction.KEEP_LEFT, checkpoint?.direction, "slight left -> keep left")
        assertEquals("Elm Avenue", checkpoint?.roadName, "road name")
    }

    test("sharp right collapses to TURN_RIGHT") {
        val checkpoint = NavigationAnnouncementParser.parse("Sharp right turn ahead")
        assertEquals(Direction.TURN_RIGHT, checkpoint?.direction, "sharp right -> turn right")
    }

    test("imperial units convert to meters") {
        val half = NavigationAnnouncementParser.parse("In 0.5 miles, turn left")
        assertTrue((half!!.distanceMeters!! - 804.672) < 0.01, "0.5 mi ~= 804.7 m")
        val feet = NavigationAnnouncementParser.parse("In 500 feet, turn right")
        assertTrue((feet!!.distanceMeters!! - 152.4) < 0.01, "500 ft = 152.4 m")
    }

    test("comma decimal separator (European) is handled") {
        val checkpoint = NavigationAnnouncementParser.parse("In 1,5 km, turn left")
        assertEquals(1500.0, checkpoint?.distanceMeters, "1,5 km = 1500 m")
    }

    test("'Head north on Oak Street' parses as STRAIGHT with a road") {
        val checkpoint = NavigationAnnouncementParser.parse("Head north on Oak Street")
        assertEquals(Direction.STRAIGHT, checkpoint?.direction, "head <compass> -> straight")
        assertEquals("Oak Street", checkpoint?.roadName, "road name")
    }

    test("Croatian 'nastavite ravno' parses as STRAIGHT") {
        val checkpoint = NavigationAnnouncementParser.parse("Za 300 m nastavite ravno")
        assertEquals(Direction.STRAIGHT, checkpoint?.direction, "nastavite ravno = continue straight")
        assertEquals(300.0, checkpoint?.distanceMeters, "distance")
    }

    test("trip-total-only text (no instruction) still does not parse") {
        val checkpoint = NavigationAnnouncementParser.parse("1.2 km | 14 min | 12:41")
        assertEquals(null, checkpoint, "a bare distance with no maneuver keyword is not a checkpoint")
    }
}
