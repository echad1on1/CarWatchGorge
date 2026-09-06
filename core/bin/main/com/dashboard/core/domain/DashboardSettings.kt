package com.dashboard.core.domain

/** Which VehicleData field a settings slot refers to. String-keyed so new signals don't require enum changes. */
typealias VehicleFieldKey = String

object VehicleFieldKeys {
    const val SPEED = "speed"
    const val RPM = "rpm"
    const val GEAR = "gear"
    const val COOLANT_TEMP = "coolant_temp"
    const val OIL_PRESSURE = "oil_pressure"
    const val OIL_TEMP = "oil_temp"
    const val ENGINE_LOAD = "engine_load"
    const val FUEL_LEVEL = "fuel_level"
    const val BATTERY_VOLTAGE = "battery_voltage"
}

enum class CarLayoutPreset { MINIMAL, PERFORMANCE, CUSTOM }

/**
 * User-controlled arrangement of the Car panel. Independent of the UI layer so it can be
 * persisted (later) without any Android/Compose dependency.
 */
data class CarPanelSettings(
    val preset: CarLayoutPreset = CarLayoutPreset.MINIMAL,
    /** Ordered list of fields to show, front to back. Only meaningful when preset == CUSTOM. */
    val visibleFields: List<VehicleFieldKey> = listOf(
        VehicleFieldKeys.SPEED,
        VehicleFieldKeys.RPM,
        VehicleFieldKeys.GEAR,
        VehicleFieldKeys.COOLANT_TEMP,
        VehicleFieldKeys.OIL_PRESSURE,
    ),
)

data class DashboardSettings(
    val carPanel: CarPanelSettings = CarPanelSettings(),
    // Future: MapsSettings, MusicSettings, BlizzerSettings, AudioSettings.
)
