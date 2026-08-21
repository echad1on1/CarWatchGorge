package com.dashboard.core.hardware

import com.dashboard.core.domain.PowerState

/**
 * Abstracts the vehicle's power signal (ignition/accessory line). A real implementation will
 * notify the app when the car is turned on/off; for now a mock drives it manually so the rest
 * of the app can be built and tested against [PowerState] without real electronics.
 */
interface PowerProvider {
    fun observePowerState(listener: (PowerState) -> Unit): Subscription
    fun currentState(): PowerState
}
