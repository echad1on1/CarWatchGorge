package com.dashboard.core.tests

import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.service.NavigationManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

fun navigationCountdownSuite() = TestSuite("NavigationManager countdown interpolation").apply {

    test("a checkpoint alone does not move distance (no baseline tick yet)") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        phone.announceNavigation("In 200 meters, turn left")
        assertEquals(200.0, manager.latest.distanceMeters, "should still be exactly the checkpoint value")
    }

    test("counts distance down based on elapsed time and speed") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        phone.announceNavigation("In 200 meters, turn left")

        val t0 = 10_000L
        manager.onVehicleSpeedTick(speedKmh = 36.0, nowMillis = t0) // establishes baseline, no movement yet
        assertEquals(200.0, manager.latest.distanceMeters, "first tick just establishes the baseline")

        // 36 km/h = 10 m/s. 5 seconds later -> 50m traveled.
        manager.onVehicleSpeedTick(speedKmh = 36.0, nowMillis = t0 + 5_000)
        assertEquals(150.0, manager.latest.distanceMeters, "should have counted down by 50m over 5s at 36km/h")
    }

    test("floors at zero, never goes negative") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        phone.announceNavigation("In 20 meters, turn left")

        val t0 = 0L
        manager.onVehicleSpeedTick(speedKmh = 36.0, nowMillis = t0)
        // 36km/h = 10m/s, 10 seconds -> 100m traveled, but only 20m remained.
        manager.onVehicleSpeedTick(speedKmh = 36.0, nowMillis = t0 + 10_000)

        assertEquals(0.0, manager.latest.distanceMeters, "should floor at 0, not go negative")
    }

    test("a fresh checkpoint resets the countdown baseline, overriding estimation drift") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        phone.announceNavigation("In 200 meters, turn left")

        val t0 = 0L
        manager.onVehicleSpeedTick(speedKmh = 36.0, nowMillis = t0)
        manager.onVehicleSpeedTick(speedKmh = 36.0, nowMillis = t0 + 3_000) // -> 170m estimated

        phone.announceNavigation("In 500 meters, turn right") // a fresh, more distant checkpoint
        assertEquals(500.0, manager.latest.distanceMeters, "fresh checkpoint should override the estimate immediately")

        // Next tick should count down from the NEW checkpoint's baseline, not compound on old timing.
        manager.onVehicleSpeedTick(speedKmh = 36.0, nowMillis = t0 + 4_000)
        assertEquals(500.0, manager.latest.distanceMeters, "the tick right after a fresh checkpoint only establishes baseline")
    }

    test("does not interpolate while navigation is inactive") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        // No announceNavigation call — navigation stays inactive.
        manager.onVehicleSpeedTick(speedKmh = 100.0, nowMillis = 0L)
        manager.onVehicleSpeedTick(speedKmh = 100.0, nowMillis = 10_000L)
        assertTrue(!manager.latest.active, "should remain inactive; ticks should be no-ops")
    }
}
