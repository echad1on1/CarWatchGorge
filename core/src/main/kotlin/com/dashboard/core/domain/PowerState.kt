package com.dashboard.core.domain

/**
 * Power lifecycle of the dashboard device itself, independent of ignition electronics.
 * A future [com.dashboard.core.hardware.PowerProvider] implementation will report these
 * based on real ignition/accessory power; for now [com.dashboard.core.hardware.mock.MockPowerProvider]
 * (added in the power-management step) will drive it.
 */
enum class PowerState { ACTIVE, IDLE, SLEEP, WAKE }
