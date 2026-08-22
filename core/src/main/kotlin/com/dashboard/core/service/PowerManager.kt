package com.dashboard.core.service

import com.dashboard.core.domain.PowerState
import com.dashboard.core.hardware.PowerProvider
import com.dashboard.core.hardware.Subscription

/**
 * Reacts to the vehicle's power lifecycle. A real implementation will drive [PowerProvider] from
 * ignition/accessory power; [com.dashboard.core.hardware.mock.MockPowerProvider] drives it via a
 * dev control ("Simulate car on/off") for now.
 *
 * This manager doesn't touch the UI or any panel directly — it exposes [onActive]/[onSleep]
 * callbacks so whatever composes the app (today: [com.dashboard.core.demo.ConsoleDemo], later:
 * the Wear OS entry point) can decide what "active" and "sleep" mean for it, e.g. starting/
 * stopping [VehicleDataManager] and [ConnectionManager]. Keeping that decision outside this class
 * means PowerManager itself has zero dependency on any other manager.
 */
class PowerManager(
    private val provider: PowerProvider,
    private val onActive: () -> Unit = {},
    private val onIdle: () -> Unit = {},
    private val onSleep: () -> Unit = {},
    private val onWake: () -> Unit = {},
) {
    var state: PowerState = provider.currentState()
        private set

    private var sub: Subscription? = null

    fun start() {
        sub = provider.observePowerState { newState -> onStateChanged(newState) }
    }

    fun stop() {
        sub?.cancel()
    }

    private fun onStateChanged(newState: PowerState) {
        state = newState
        when (newState) {
            PowerState.ACTIVE -> onActive()
            PowerState.IDLE -> onIdle()
            PowerState.SLEEP -> onSleep()
            PowerState.WAKE -> onWake()
        }
    }
}
