package com.dashboard.core.demo

import com.dashboard.core.domain.CarLayoutPreset
import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.domain.PowerState
import com.dashboard.core.domain.Signal
import com.dashboard.core.domain.VehicleData
import com.dashboard.core.hardware.mock.InMemorySettingsStore
import com.dashboard.core.hardware.mock.MockAudioOutput
import com.dashboard.core.hardware.mock.MockBluetoothProvider
import com.dashboard.core.hardware.mock.MockNfcProvider
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.hardware.mock.MockPowerProvider
import com.dashboard.core.hardware.mock.MockVehicleDataProvider
import com.dashboard.core.service.BlizzerAudioManager
import com.dashboard.core.service.BlizzerManager
import com.dashboard.core.service.ConnectionManager
import com.dashboard.core.service.DevControlPanel
import com.dashboard.core.service.MediaManager
import com.dashboard.core.service.NavigationAudioManager
import com.dashboard.core.service.NavigationManager
import com.dashboard.core.service.PowerManager
import com.dashboard.core.service.SettingsManager
import com.dashboard.core.service.VehicleDataManager

/**
 * Text-mode stand-in for the real dashboard UI.
 *
 * There is no Android/Wear OS SDK available in this development sandbox (no network access to
 * Google's Maven repository), so this console renderer is how every subsystem is exercised and
 * visually verified today. It composes the app EXACTLY the way the real Wear OS entry point
 * (`wearos-app`'s `MainActivity`) does: build the hardware mocks, wrap each in its manager, wire
 * [PowerManager] to start/stop the rest, wire the vehicle's live speed into
 * [NavigationManager.onVehicleSpeedTick] for smooth distance countdown, and drive everything
 * through [DevControlPanel] — the same facade the real dev-tools screen binds to.
 *
 * Run: see tools/run_demo.sh
 */
fun main() {
    // ---- Hardware layer (mocks) --------------------------------------------------------
    val vehicleProvider = MockVehicleDataProvider()
    val nfcProvider = MockNfcProvider()
    val bluetoothProvider = MockBluetoothProvider(connectDelayMillis = 300)
    val phoneCommunication = MockPhoneCommunication()
    val audioOutput = MockAudioOutput()
    val powerProvider = MockPowerProvider(initial = PowerState.ACTIVE)
    val settingsStore = InMemorySettingsStore()

    // ---- Service / domain layer ----------------------------------------------------------
    val vehicleManager = VehicleDataManager(vehicleProvider)
    val connectionManager = ConnectionManager(nfcProvider, bluetoothProvider)
    val navigationManager = NavigationManager(phoneCommunication)
    val navigationAudioManager = NavigationAudioManager(navigationManager, audioOutput)
    val mediaManager = MediaManager(phoneCommunication)
    val blizzerManager = BlizzerManager(phoneCommunication)
    val blizzerAudioManager = BlizzerAudioManager(blizzerManager, audioOutput)
    val settingsManager = SettingsManager(settingsStore)

    // The Maps strategy in one line: every vehicle-data tick feeds the current speed into
    // NavigationManager, which counts distance-to-next-turn down smoothly between the discrete
    // announcement checkpoints announceNavigation() sets. Watch the [Maps] lines below shrink
    // between "Announce" steps even though only two announcements are made.
    vehicleManager.observe { data ->
        (data.speedKmh as? Signal.Available)?.let { navigationManager.onVehicleSpeedTick(it.value) }
    }

    // PowerManager decides what "active" and "sleep" mean for the rest of the app. Neither
    // VehicleDataManager nor ConnectionManager know PowerManager exists.
    val powerManager = PowerManager(
        provider = powerProvider,
        onActive = {
            println("\n[power] ACTIVE — waking dashboard")
            vehicleManager.start()
            connectionManager.start()
            navigationManager.start()
            mediaManager.start()
            blizzerManager.start()
        },
        onSleep = {
            println("\n[power] SLEEP — dashboard going dark")
            vehicleManager.stop()
            connectionManager.stop()
            navigationManager.stop()
            mediaManager.stop()
            blizzerManager.stop()
        },
    )

    // The single surface a future "developer tools" screen would bind to.
    val devControls = DevControlPanel(connectionManager, nfcProvider, vehicleProvider, phoneCommunication, powerProvider)

    // ---- Wire up rendering (stand-in for panels) ------------------------------------------
    connectionManager.observeState { state -> println("\n=== connection state -> $state ===") }
    vehicleManager.observe { data -> render(connectionManager.state, data) }
    navigationManager.observe { nav ->
        if (nav.active) {
            println("  [Maps] ${nav.direction} in ${nav.distanceMeters?.toInt()}m on ${nav.roadName}, ETA ${nav.etaMinutes}min")
        } else {
            println("  [Maps] navigation not running")
        }
    }
    mediaManager.observe { media ->
        if (media.title != null) println("  [Music] ${media.playbackState}: \"${media.title}\" - ${media.artist}")
    }
    blizzerManager.observe { event ->
        if (event != null) println("  [BLIZZER OVERLAY] (${event.type}) ${event.message}")
    }
    settingsManager.observe { settings -> println("  [Settings] Car panel preset = ${settings.carPanel.preset}") }

    // ---- Boot -------------------------------------------------------------------------------
    powerManager.start()
    navigationAudioManager.start()
    blizzerAudioManager.start()

    println("Dashboard booting... (Car panel works with no phone at all)")
    sleep(1500)

    println("\n[dev] simulating driver accelerating to 80 km/h")
    devControls.setTargetSpeedKmh(80.0)
    sleep(1500)

    println("\n[dev] user changes Car panel layout preset")
    settingsManager.updateCarPanel { it.copy(preset = CarLayoutPreset.PERFORMANCE) }
    sleep(300)

    println("\n[dev] Camera Warning: 500m (Blizzer overlays the Car panel)")
    devControls.triggerCameraWarning(500)
    sleep(800)
    devControls.dismissCameraWarning()
    sleep(300)

    println("\n[dev] Simulate NFC Tap")
    devControls.simulateNfcTap()
    sleep(1000) // let CONNECTING -> CONNECTED resolve

    println("\n[dev] Maps/Music now reachable.")
    println("[dev] Announce: \"In 500 meters, turn right onto Ridge Valley Rd\" (the Maps strategy in action)")
    devControls.announceNavigation("In 500 meters, turn right onto Ridge Valley Rd")
    sleep(500)

    println("\n[dev] ...driving continues; watch distance count down from vehicle speed alone, no new announcement yet...")
    sleep(2000)

    println("\n[dev] Announce: \"Turn right\" (closer checkpoint, direction confirmed, road name carried over)")
    devControls.announceNavigation("Turn right")
    sleep(500)

    println("\n[dev] Start Music")
    devControls.startMusic()
    sleep(500)

    println("\n[dev] Next Song")
    devControls.nextSong()
    sleep(500)

    println("\n[dev] Camera Warning approaching: 500m -> 200m -> 100m (blink/beep should escalate)")
    devControls.triggerCameraWarning(500)
    sleep(400)
    devControls.triggerCameraWarning(200)
    sleep(400)
    devControls.triggerCameraWarning(100)
    sleep(800)
    devControls.dismissCameraWarning()
    sleep(300)

    println("\n[dev] Announce: \"You have arrived at your destination\"")
    devControls.announceNavigation("You have arrived at your destination")
    sleep(500)
    devControls.pauseMusic()
    sleep(500)

    println("\n[dev] Disconnect Phone")
    devControls.disconnectPhone()
    sleep(500)

    println("\n[dev] Simulate car turning off (PowerManager -> SLEEP)")
    devControls.simulatePowerState(PowerState.SLEEP)
    sleep(500)

    println("\nDashboard asleep. Demo complete.")
    navigationAudioManager.stop()
    blizzerAudioManager.stop()
    powerManager.stop()
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
