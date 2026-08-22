package com.dashboard.core.demo

import com.dashboard.core.domain.BlizzerEventType
import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.domain.Direction
import com.dashboard.core.domain.Signal
import com.dashboard.core.domain.VehicleData
import com.dashboard.core.hardware.mock.MockBluetoothProvider
import com.dashboard.core.hardware.mock.MockNfcProvider
import com.dashboard.core.hardware.mock.MockPhoneCommunication
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

    // The phone-communication layer. In the real app this would be a BluetoothPhoneCommunication
    // wired to the same BluetoothProvider ConnectionManager uses; MockPhoneCommunication is used
    // directly here as the developer-controls convenience described in the spec. Both implement
    // the same PhoneCommunication interface, so panels (once built) can't tell the difference.
    val phoneCommunication = MockPhoneCommunication()

    connectionManager.observeState { state ->
        println("\n=== connection state -> $state ===")
    }
    vehicleManager.observe { data -> render(connectionManager.state, data) }
    phoneCommunication.observeNavigationState { nav ->
        if (nav.active) {
            println("  [Maps] ${nav.direction} in ${nav.distanceMeters?.toInt()}m on ${nav.roadName}, ETA ${nav.etaMinutes}min")
        } else {
            println("  [Maps] navigation not running")
        }
    }
    phoneCommunication.observeMediaState { media ->
        if (media.title != null) {
            println("  [Music] ${media.playbackState}: \"${media.title}\" - ${media.artist}")
        }
    }
    phoneCommunication.observeBlizzerEvents { event ->
        println("  [BLIZZER OVERLAY] (${event.type}) ${event.message}")
    }

    vehicleManager.start()
    connectionManager.start()

    println("Dashboard booting... (Car panel works with no phone at all)")
    sleep(1500)

    println("\n[dev] simulating driver accelerating to 80 km/h")
    vehicleProvider.setTargetSpeedKmh(80.0)
    sleep(1500)

    println("\n[dev] Trigger Blizzer while on Car panel")
    phoneCommunication.triggerBlizzer("Welcome back!", BlizzerEventType.INFO)
    sleep(500)

    println("\n[dev] Simulate NFC Tap")
    nfcProvider.simulateTap()
    sleep(1000) // let CONNECTING -> CONNECTED resolve

    println("\n[dev] Maps/Music now reachable. Start Navigation")
    phoneCommunication.startNavigation(roadName = "Ridge Valley Rd", etaMinutes = 9)
    sleep(500)

    println("\n[dev] Change Direction")
    phoneCommunication.changeDirection(Direction.TURN_RIGHT, distanceMeters = 150.0)
    sleep(500)

    println("\n[dev] Start Music")
    phoneCommunication.startMusic()
    sleep(500)

    println("\n[dev] Next Song")
    phoneCommunication.nextSong()
    sleep(500)

    println("\n[dev] Trigger Blizzer while on Maps/Music")
    phoneCommunication.triggerBlizzer("Check engine soon", BlizzerEventType.WARNING)
    sleep(500)

    println("\n[dev] Stop Navigation, Pause Music")
    phoneCommunication.stopNavigation()
    phoneCommunication.pauseMusic()
    sleep(500)

    println("\n[dev] Disconnect Phone")
    connectionManager.simulateDisconnect()
    sleep(500)

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
