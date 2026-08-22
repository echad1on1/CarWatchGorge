package com.dashboard.core.tests

import com.dashboard.core.domain.PowerState
import com.dashboard.core.hardware.mock.MockPowerProvider
import com.dashboard.core.service.PowerManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

fun powerManagerSuite() = TestSuite("PowerManager").apply {

    test("reflects the provider's initial state") {
        val provider = MockPowerProvider(initial = PowerState.ACTIVE)
        val manager = PowerManager(provider)
        assertEquals(PowerState.ACTIVE, manager.state, "should reflect initial provider state")
    }

    test("start() immediately invokes the callback for the current state (e.g. onActive at boot)") {
        val provider = MockPowerProvider(initial = PowerState.ACTIVE)
        var activeCalled = false
        val manager = PowerManager(provider, onActive = { activeCalled = true })
        manager.start()
        assertTrue(activeCalled, "onActive should fire immediately on start() when already ACTIVE")
    }

    test("calls onSleep when the provider reports SLEEP") {
        val provider = MockPowerProvider(initial = PowerState.ACTIVE)
        var sleptCalled = false
        val manager = PowerManager(provider, onSleep = { sleptCalled = true })
        manager.start()

        provider.setState(PowerState.SLEEP)

        assertTrue(sleptCalled, "onSleep should have been invoked")
        assertEquals(PowerState.SLEEP, manager.state, "state should update to SLEEP")
    }

    test("calls onWake when the provider reports WAKE") {
        val provider = MockPowerProvider(initial = PowerState.SLEEP)
        var wokeCalled = false
        val manager = PowerManager(provider, onWake = { wokeCalled = true })
        manager.start()

        provider.setState(PowerState.WAKE)

        assertTrue(wokeCalled, "onWake should have been invoked")
    }

    test("stop() unsubscribes so further changes are ignored") {
        val provider = MockPowerProvider(initial = PowerState.ACTIVE)
        var callCount = 0
        val manager = PowerManager(provider, onSleep = { callCount++ })
        manager.start()
        manager.stop()

        provider.setState(PowerState.SLEEP)

        assertEquals(0, callCount, "should not react to changes after stop()")
    }
}
