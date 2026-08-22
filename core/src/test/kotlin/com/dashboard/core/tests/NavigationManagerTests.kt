package com.dashboard.core.tests

import com.dashboard.core.domain.Direction
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.service.NavigationManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertFalse

fun navigationManagerSuite() = TestSuite("NavigationManager").apply {

    test("starts inactive when no navigation has been reported") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        assertFalse(manager.latest.active, "should start inactive")
    }

    test("reflects startNavigation from the phone side") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        phone.startNavigation(roadName = "Elm St", etaMinutes = 5)
        assertEquals("Elm St", manager.latest.roadName, "should reflect the phone's navigation state")
    }

    test("late subscriber gets the current snapshot immediately") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        phone.startNavigation(roadName = "Coast Hwy")
        phone.changeDirection(Direction.TURN_LEFT, 200.0)

        var received: Direction? = null
        manager.observe { received = it.direction }
        assertEquals(Direction.TURN_LEFT, received, "late subscriber should see current direction immediately")
    }

    test("stopNavigation reflects as active = false") {
        val phone = MockPhoneCommunication()
        val manager = NavigationManager(phone)
        manager.start()
        phone.startNavigation()
        phone.stopNavigation()
        assertFalse(manager.latest.active, "should be inactive after stopNavigation")
    }
}
