package com.dashboard.core.tests

import com.dashboard.core.domain.BlizzerEventType
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.service.BlizzerManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

fun blizzerManagerSuite() = TestSuite("BlizzerManager").apply {

    test("starts with no active event") {
        val phone = MockPhoneCommunication()
        val manager = BlizzerManager(phone)
        manager.start()
        assertEquals(null, manager.currentEvent, "should start with nothing active")
    }

    test("triggerBlizzer sets currentEvent") {
        val phone = MockPhoneCommunication()
        val manager = BlizzerManager(phone)
        manager.start()
        phone.triggerBlizzer("Low tire pressure", BlizzerEventType.WARNING)
        assertTrue(manager.currentEvent != null, "should have an active event")
        assertEquals("Low tire pressure", manager.currentEvent!!.message, "message should match")
    }

    test("dismissBlizzer clears currentEvent, same behavior regardless of which panel is active") {
        val phone = MockPhoneCommunication()
        val manager = BlizzerManager(phone)
        manager.start()
        val id = phone.triggerBlizzer("Check engine")
        phone.dismissBlizzer(id)
        assertEquals(null, manager.currentEvent, "should be cleared after dismiss, with no panel awareness needed")
    }

    test("late subscriber immediately receives the current active event") {
        val phone = MockPhoneCommunication()
        val manager = BlizzerManager(phone)
        manager.start()
        phone.triggerBlizzer("Welcome back")

        var received: String? = null
        manager.observe { received = it?.message }
        assertEquals("Welcome back", received, "late subscriber should see the active event immediately")
    }

    test("late subscriber receives null when nothing is active") {
        val phone = MockPhoneCommunication()
        val manager = BlizzerManager(phone)
        manager.start()

        var receivedCalled = false
        manager.observe { receivedCalled = true; assertEquals(null, it, "should be null") }
        assertTrue(receivedCalled, "listener should be invoked immediately even with nothing active")
    }
}
