package com.dashboard.core.domain

/**
 * Wraps a single vehicle parameter (e.g. RPM, coolant temperature).
 *
 * Different vehicles expose different OBD-II / CAN parameters. Rather than
 * having the UI guess or default missing values to 0, every value on
 * [VehicleData] is wrapped in [Signal] so the UI can explicitly render
 * "unavailable" instead of a fabricated number.
 *
 * This type is intentionally hardware-independent: it says nothing about
 * OBD-II, CAN bus IDs, or PIDs. That knowledge belongs entirely inside a
 * concrete [com.dashboard.core.hardware.VehicleDataProvider] implementation.
 */
sealed class Signal<out T> {
    data class Available<T>(val value: T, val timestampMillis: Long) : Signal<T>()
    data object Unavailable : Signal<Nothing>()

    val isAvailable: Boolean get() = this is Available<T>

    /** Returns the value if available, otherwise [fallback]. Never invents data silently. */
    fun orElse(fallback: @UnsafeVariance T): T = when (this) {
        is Available -> value
        Unavailable -> fallback
    }
}

/** Discrete gear position. PARK/REVERSE/NEUTRAL/DRIVE cover automatics; NUMBERED covers manuals/paddle. */
enum class GearPosition { PARK, REVERSE, NEUTRAL, DRIVE, NUMBERED, UNKNOWN }

data class Gear(val position: GearPosition, val numberedGear: Int? = null)

/**
 * Snapshot of everything the dashboard currently knows about the vehicle.
 *
 * Every field is a [Signal] so a vehicle that doesn't expose, say, oil
 * pressure produces [Signal.Unavailable] rather than a fake reading of 0.
 * Fields can be added over time without breaking existing consumers because
 * they are all optional/defaulted to Unavailable.
 */
data class VehicleData(
    val speedKmh: Signal<Double> = Signal.Unavailable,
    val rpm: Signal<Int> = Signal.Unavailable,
    val gear: Signal<Gear> = Signal.Unavailable,
    val coolantTempCelsius: Signal<Double> = Signal.Unavailable,
    val oilPressureKpa: Signal<Double> = Signal.Unavailable,
    val oilTempCelsius: Signal<Double> = Signal.Unavailable,
    val engineLoadPercent: Signal<Double> = Signal.Unavailable,
    val fuelLevelPercent: Signal<Double> = Signal.Unavailable,
    val batteryVoltage: Signal<Double> = Signal.Unavailable,
    /** Extension point: vehicle-specific values that don't warrant a dedicated field yet. */
    val extras: Map<String, Signal<Double>> = emptyMap(),
)
