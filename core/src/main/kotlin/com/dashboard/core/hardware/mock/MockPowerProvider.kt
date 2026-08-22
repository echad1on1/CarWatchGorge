package com.dashboard.core.hardware.mock

import com.dashboard.core.domain.PowerState
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.PowerProvider
import com.dashboard.core.hardware.Subscription

/**
 * Placeholder power source. Defaults to ACTIVE ("car is on") so the rest of the app can be
 * developed without worrying about power yet. [setState] will become the dev control
 * ("Simulate car on/off") once the PowerManager step wires this up properly.
 */
class MockPowerProvider(initial: PowerState = PowerState.ACTIVE) : PowerProvider {
    private val emitter = Emitter<PowerState>()
    private var state = initial

    override fun observePowerState(listener: (PowerState) -> Unit): Subscription {
        // Deliver the current state immediately, matching the "latest snapshot to late
        // subscribers" convention used by every other manager in this codebase
        // (VehicleDataManager, NavigationManager, MediaManager, BlizzerManager, ...).
        listener(state)
        return emitter.subscribe(listener)
    }

    override fun currentState(): PowerState = state

    fun setState(newState: PowerState) {
        state = newState
        emitter.emit(newState)
    }
}
