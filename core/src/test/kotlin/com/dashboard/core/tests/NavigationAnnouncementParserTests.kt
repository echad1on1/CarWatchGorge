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
        assertEquals(Direction.ROUNDABOUT, checkpoint!!.direction, "direction")
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
}
