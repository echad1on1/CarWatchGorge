package com.dashboard.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dashboard.core.domain.PowerState
import com.dashboard.core.domain.Signal
import com.dashboard.core.hardware.mock.InMemorySettingsStore
import com.dashboard.core.hardware.mock.MockAudioOutput
import com.dashboard.core.hardware.mock.MockBluetoothProvider
import com.dashboard.core.hardware.mock.MockNfcProvider
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.hardware.mock.MockPowerProvider
import com.dashboard.core.hardware.mock.MockVehicleDataProvider
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
 * Entry point. Composes the app EXACTLY the way
 * [com.dashboard.core.demo.ConsoleDemo] does — same hardware mocks, same managers, same
 * [DevControlPanel] facade — but renders real Compose UI ([DashboardApp]) instead of println.
 *
 * IMPORTANT: this still uses the *mock* hardware implementations. Real NFC/Bluetooth/vehicle
 * implementations are a separate, later step — see docs/android-integration-research.md and
 * this module's manifest comments before building them. Swapping a mock for a real
 * implementation here should require touching only this file (constructing a different
 * `XyzProvider`), never `core/` or the Compose screens.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---- Hardware layer (mocks — see class doc) ----------------------------------------
        val vehicleProvider = MockVehicleDataProvider()
        val nfcProvider = MockNfcProvider()
        val bluetoothProvider = MockBluetoothProvider(connectDelayMillis = 300)
        val phoneCommunication = MockPhoneCommunication()
        val audioOutput = MockAudioOutput()
        val powerProvider = MockPowerProvider(initial = PowerState.ACTIVE)
        val settingsStore = InMemorySettingsStore()

        // ---- Service / domain layer -----------------------------------------------------------
        val vehicleManager = VehicleDataManager(vehicleProvider)
        val connectionManager = ConnectionManager(nfcProvider, bluetoothProvider)
        val navigationManager = NavigationManager(phoneCommunication)
        val navigationAudioManager = NavigationAudioManager(navigationManager, audioOutput)
        val mediaManager = MediaManager(phoneCommunication)
        val blizzerManager = BlizzerManager(phoneCommunication)
        val settingsManager = SettingsManager(settingsStore)

        // Feeds the vehicle's live speed into NavigationManager so distance-to-next-turn counts
        // down smoothly between announcement checkpoints (see NavigationManager's class doc) —
        // this is what makes the "watch shows a live countdown, not just discrete jumps" idea real.
        vehicleManager.observe { data ->
            (data.speedKmh as? Signal.Available)?.let { navigationManager.onVehicleSpeedTick(it.value) }
        }

        val powerManager = PowerManager(
            provider = powerProvider,
            onActive = {
                vehicleManager.start()
                connectionManager.start()
                navigationManager.start()
                mediaManager.start()
                blizzerManager.start()
            },
            onSleep = {
                vehicleManager.stop()
                connectionManager.stop()
                navigationManager.stop()
                mediaManager.stop()
                blizzerManager.stop()
            },
        )

        val devControls = DevControlPanel(connectionManager, nfcProvider, vehicleProvider, phoneCommunication, powerProvider)

        powerManager.start()
        navigationAudioManager.start()

        setContent {
            DashboardApp(
                vehicleManager = vehicleManager,
                connectionManager = connectionManager,
                navigationManager = navigationManager,
                mediaManager = mediaManager,
                blizzerManager = blizzerManager,
                settingsManager = settingsManager,
                devControls = devControls,
            )
        }
    }
}
