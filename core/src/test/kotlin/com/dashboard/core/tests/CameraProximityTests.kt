package com.dashboard.core.tests

import com.dashboard.core.blizzer.CameraPoi
import com.dashboard.core.blizzer.CameraProximity
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue
import kotlin.math.abs

fun cameraProximitySuite() = TestSuite("CameraProximity").apply {

    test("haversine matches a known short distance") {
        // ~1 arc-minute of latitude ≈ 1852 m.
        val d = CameraProximity.haversineMeters(45.0, 15.0, 45.0 + 1.0 / 60.0, 15.0)
        assertTrue(abs(d - 1852.0) < 5.0, "1' latitude ~= 1852 m, got $d")
    }

    test("nearest returns the closest camera inside the range and null outside") {
        val here = 45.8150 to 15.9819 // central Zagreb
        val cameras = listOf(
            CameraPoi("far", 45.9000, 16.1000),
            CameraPoi("near", 45.8160, 15.9825, roadName = "Vukovarska"),
            CameraPoi("mid", 45.8250, 15.9900),
        )
        val hit = CameraProximity.nearest(here.first, here.second, cameras, maxMeters = 1000.0)
        assertEquals("near", hit?.first?.id, "closest within 1 km")
        assertTrue(hit!!.second < 200.0, "and it's well under 200 m")

        val nothing = CameraProximity.nearest(0.0, 0.0, cameras, maxMeters = 1000.0)
        assertEquals(null, nothing, "no camera near null island")
    }

    test("crossedThreshold fires once per band on an inward approach") {
        val t = CameraProximity.DEFAULT_THRESHOLDS
        assertEquals(2000, CameraProximity.crossedThreshold(null, 1900.0, t), "first sight inside 2 km")
        assertEquals(1000, CameraProximity.crossedThreshold(1900.0, 950.0, t), "enters 1 km")
        assertEquals(500, CameraProximity.crossedThreshold(950.0, 480.0, t), "enters 500 m")
        assertEquals(null, CameraProximity.crossedThreshold(480.0, 300.0, t), "still in the 500 m band")
        assertEquals(200, CameraProximity.crossedThreshold(300.0, 150.0, t), "enters 200 m")
        assertEquals(100, CameraProximity.crossedThreshold(150.0, 60.0, t), "enters 100 m")
    }

    test("crossedThreshold: a fast approach jumps straight to the tightest band crossed") {
        assertEquals(100, CameraProximity.crossedThreshold(2500.0, 80.0, CameraProximity.DEFAULT_THRESHOLDS),
            "2.5 km -> 80 m in one fix fires the 100 m band")
    }

    test("crossedThreshold: jitter around a boundary does not re-fire") {
        val t = CameraProximity.DEFAULT_THRESHOLDS
        assertEquals(500, CameraProximity.crossedThreshold(600.0, 470.0, t), "enters 500 m")
        assertEquals(null, CameraProximity.crossedThreshold(470.0, 498.0, t), "drift back out to 498 m — no fire")
        assertEquals(null, CameraProximity.crossedThreshold(498.0, 501.0, t), "still no fire at 501 m")
    }

    test("crossedThreshold returns null when well outside every band") {
        assertEquals(null, CameraProximity.crossedThreshold(null, 5000.0, CameraProximity.DEFAULT_THRESHOLDS))
    }

    test("toBlizzerEvent / clearedEvent shape") {
        val poi = CameraPoi("c1", 45.0, 15.0, roadName = "A3")
        val ev = CameraProximity.toBlizzerEvent(poi, 200, "evt-1", 1_000L)
        assertEquals("evt-1", ev.id, "id")
        assertEquals(200, ev.distanceMeters, "threshold carried as distanceMeters")
        assertEquals(true, ev.active, "active")
        assertTrue(ev.message.contains("A3"), "message names the road")

        val cleared = CameraProximity.clearedEvent("evt-1", 2_000L)
        assertEquals(false, cleared.active, "cleared event is inactive")
        assertEquals("evt-1", cleared.id, "same id so BlizzerManager clears the right one")
    }
}
