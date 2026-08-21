package com.dashboard.core.demo

import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.domain.Signal
import com.dashboard.core.domain.VehicleData
import com.dashboard.core.hardware.mock.MockBluetoothProvider
import com.dashboard.core.hardware.mock.MockNfcProvider
import com.dashboard.core.hardware.mock.MockVehicleDataProvider
import com.dashboard.core.service.ConnectionManager
import com.dashboard.core.service.VehicleDataManager

/**
 * Text-mode stand-in for the real dashboard UI.
 *
 * There is no Android/Wear OS SDK available in this development sandbox (no network access to
 * Google's Maven repository), so this console renderer is how the Car panel + connection state
 * machine are exercised and visually verified today. It talks to the exact same
 * [VehicleDataManager] / [ConnectionManager] service layer that a Jetpack Compose Wear OS UI
 * will talk to later — swapping this file for real Compose screens should require zero changes
 * below the service layer.
 *
 * Run: see tools/run_demo.sh
 */
fun main() {
    val vehicleProvider = MockVehicleDataProvider()
    val vehicleManager = VehicleDataManager(vehicleProvider)

    val nfcProvider = MockNfcProvider()
    val bluetoothProvider = MockBluetoothProvider(connectDelayMillis = 300)
    val connectionManager = ConnectionManager(nfcProvider, bluetoothProvider)

    connectionManager.observeState { state ->
        println("\n=== connection state -> $state ===")
    }
    vehicleManager.observe { data -> render(connectionManager.state, data) }

    vehicleManager.start()
    connectionManager.start()

    println("Dashboard booting... (Car panel works with no phone at all)")
    sleep(2000)

    println("\n[dev] simulating driver accelerating to 80 km/h")
    vehicleProvider.setTargetSpeedKmh(80.0)
    sleep(2000)

    println("\n[dev] Simulate NFC Tap")
    nfcProvider.simulateTap()
    sleep(1500) // let CONNECTING -> CONNECTED resolve

    println("\n[dev] (Maps/Music would now be reachable; built in later steps)")
    sleep(1500)

    println("\n[dev] Disconnect Phone")
    connectionManager.simulateDisconnect()
    sleep(1000)

    println("\nBack to Car-only mode. Shutting down demo.")
    vehicleManager.stop()
    connectionManager.stop()
}

private fun render(connectionState: ConnectionState, data: VehicleData) {
    fun <T> fmt(signal: Signal<T>): String = when (signal) {
        is Signal.Available -> signal.value.toString()
        Signal.Unavailable -> "—"
    }
    println(
        "[$connectionState] speed=${fmt(data.speedKmh)}km/h rpm=${fmt(data.rpm)} " +
            "gear=${fmt(data.gear)} coolant=${fmt(data.coolantTempCelsius)}C " +
            "oilPressure=${fmt(data.oilPressureKpa)} fuel=${fmt(data.fuelLevelPercent)}%"
    )
}

private fun sleep(millis: Long) = Thread.sleep(millis)
