package com.dashboard.core.hardware.mock

import com.dashboard.core.domain.Gear
import com.dashboard.core.domain.GearPosition
import com.dashboard.core.domain.Signal
import com.dashboard.core.domain.VehicleData
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.Subscription
import com.dashboard.core.hardware.VehicleDataProvider
import java.util.Timer
import java.util.TimerTask
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Simulated vehicle. Values evolve gradually and plausibly rather than jumping randomly:
 * - Speed drifts based on a simulated "driver" target (see [setTargetSpeedKmh] / dev controls).
 * - RPM reacts to speed and simulated gear.
 * - Coolant temp climbs slowly toward operating temperature then hovers.
 * - Engine load tracks how hard the "driver" is accelerating.
 *
 * [oilPressureAvailable] can be toggled to demonstrate that unavailable values are never
 * fabricated — this stands in for "this vehicle doesn't expose oil pressure over OBD-II".
 */
class MockVehicleDataProvider(
    private val tickIntervalMillis: Long = 500,
    private val oilPressureAvailable: Boolean = true,
) : VehicleDataProvider {

    private val emitter = Emitter<VehicleData>()
    private var timer: Timer? = null
    private val random = Random(System.currentTimeMillis())

    // Simulated physical state.
    @Volatile private var speedKmh = 0.0
    @Volatile private var targetSpeedKmh = 0.0
    @Volatile private var coolantTemp = 20.0 // starts cold, like a real engine
    @Volatile private var fuelLevel = 72.0
    @Volatile private var batteryVoltage = 12.6

    override fun start() {
        if (timer != null) return
        timer = Timer("MockVehicleDataProvider", true).apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() = tick()
            }, 0, tickIntervalMillis)
        }
    }

    override fun stop() {
        timer?.cancel()
        timer = null
    }

    override fun observe(listener: (VehicleData) -> Unit): Subscription = emitter.subscribe(listener)

    /** Dev control: simulate the driver pressing the accelerator toward a target speed. */
    fun setTargetSpeedKmh(kmh: Double) {
        targetSpeedKmh = kmh.coerceIn(0.0, 220.0)
    }

    private fun tick() {
        // Speed drifts toward target with some noise, like real acceleration/deceleration.
        val delta = (targetSpeedKmh - speedKmh)
        speedKmh = (speedKmh + delta * 0.15 + random.nextDouble(-0.4, 0.4)).coerceIn(0.0, 220.0)

        val gear = gearForSpeed(speedKmh)
        val rpm = rpmFor(speedKmh, gear)
        val engineLoad = engineLoadFor(delta, speedKmh)

        // Coolant temp climbs toward ~90C then hovers with small noise once warm.
        val targetCoolant = 90.0
        coolantTemp = if (coolantTemp < targetCoolant) {
            min(targetCoolant, coolantTemp + 0.3 + random.nextDouble(0.0, 0.2))
        } else {
            (coolantTemp + random.nextDouble(-0.3, 0.3)).coerceIn(85.0, 100.0)
        }

        fuelLevel = max(0.0, fuelLevel - 0.001 * (1 + speedKmh / 100.0))
        batteryVoltage = (batteryVoltage + random.nextDouble(-0.02, 0.02)).coerceIn(11.8, 14.6)

        val now = System.currentTimeMillis()
        val data = VehicleData(
            speedKmh = Signal.Available(speedKmh, now),
            rpm = Signal.Available(rpm, now),
            gear = Signal.Available(gear, now),
            coolantTempCelsius = Signal.Available(coolantTemp, now),
            oilPressureKpa = if (oilPressureAvailable) {
                Signal.Available(280.0 + random.nextDouble(-15.0, 15.0), now)
            } else {
                Signal.Unavailable // this vehicle simply doesn't expose it — never fake a number
            },
            oilTempCelsius = Signal.Available(coolantTemp + random.nextDouble(-5.0, 15.0), now),
            engineLoadPercent = Signal.Available(engineLoad, now),
            fuelLevelPercent = Signal.Available(fuelLevel, now),
            batteryVoltage = Signal.Available(batteryVoltage, now),
        )
        emitter.emit(data)
    }

    private fun gearForSpeed(speed: Double): Gear = when {
        speed < 0.5 -> Gear(GearPosition.PARK)
        speed < 20 -> Gear(GearPosition.DRIVE, 1)
        speed < 45 -> Gear(GearPosition.DRIVE, 2)
        speed < 70 -> Gear(GearPosition.DRIVE, 3)
        speed < 100 -> Gear(GearPosition.DRIVE, 4)
        speed < 140 -> Gear(GearPosition.DRIVE, 5)
        else -> Gear(GearPosition.DRIVE, 6)
    }

    private fun rpmFor(speed: Double, gear: Gear): Int {
        if (gear.position == GearPosition.PARK) return 800 // idle
        val gearNum = gear.numberedGear ?: 1
        // Rough per-gear speed-to-RPM ratio, just enough to look plausible in a simulator.
        val ratio = 1800.0 / gearNum
        return (900 + speed * ratio / 10.0).toInt().coerceIn(800, 6500)
    }

    private fun engineLoadFor(accelerationSignal: Double, speed: Double): Double {
        val base = 8.0 + speed * 0.15
        val accelBoost = max(0.0, accelerationSignal) * 2.5
        return (base + accelBoost).coerceIn(0.0, 100.0)
    }
}
