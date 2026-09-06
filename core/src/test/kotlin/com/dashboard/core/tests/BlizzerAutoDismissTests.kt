package com.dashboard.core.tests

import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.service.BlizzerManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertTrue

fun blizzerAutoDismissSuite() = TestSuite("BlizzerManager auto-dismiss").apply {

    test("auto-dismisses after the configured duration with no further updates") {
        val phone = MockPhoneCommunication()
        val manager = BlizzerManager(phone, autoDismissMillis = 100)
        manager.start()
        phone.triggerCameraWarning(500)
        assertTrue(manager.currentEvent != null, "should be active immediately")
        Thread.sleep(250)
        assertTrue(manager.currentEvent == null, "should auto-dismiss after the timeout elapses")
    }

    test("a fresh update before the timeout resets the auto-dismiss timer") {
        val phone = MockPhoneCommunication()
        val manager = BlizzerManager(phone, autoDismissMillis = 200)
        manager.start()
        phone.triggerCameraWarning(1000)
        Thread.sleep(120)
        phone.triggerCameraWarning(500) // same event id, closer distance - resets the timer
        Thread.sleep(120)
        assertTrue(manager.currentEvent != null, "should still be active since an update reset the timer before 200ms elapsed")
    }

    test("stop() cancels any pending auto-dismiss") {
        val phone = MockPhoneCommunication()
        val manager = BlizzerManager(phone, autoDismissMillis = 50)
        manager.start()
        phone.triggerCameraWarning(500)
        manager.stop()
        Thread.sleep(150)
        assertTrue(manager.currentEvent == null, "stop() should already clear state; no crash from a lingering timer")
    }
}
