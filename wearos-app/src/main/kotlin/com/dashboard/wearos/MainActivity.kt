package com.dashboard.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dashboard.core.domain.PowerState
import com.dashboard.core.domain.Signal
import com.dashboard.core.communication.BluetoothPhoneCommunication
import com.dashboard.core.hardware.mock.MockAudioOutput
import com.dashboard.core.hardware.mock.MockNfcProvider
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.hardware.mock.MockPowerProvider
import com.dashboard.core.service.BlizzerManager
import com.dashboard.core.service.ConnectionManager
import com.dashboard.core.service.DevControlPanel
import com.dashboard.core.service.MediaManager
import com.dashboard.core.service.NavigationAudioManager
import com.dashboard.core.service.NavigationManager
import com.dashboard.core.service.PowerManager
import com.dashboard.core.service.SettingsManager
import com.dashboard.core.service.VehicleDataManager
import com.dashboard.wearos.hardware.DataStoreSettingsStore
import com.dashboard.wearos.hardware.WearDataLayerBluetoothProvider
import com.dashboard.wearos.hardware.vehicle.VehicleProviderFactory

/**
 * Entry point. Composes the app the way [com.dashboard.core.demo.ConsoleDemo] does — same
 * managers, same [DevControlPanel] facade — but renders real Compose UI ([DashboardApp]).
 *
 * Real data sources: navigation + phone link state via the Wear OS Data Layer
 * ([WearDataLayerBluetoothProvider]); media via the same transport (Phase 1); vehicle data via
 * a BLE OBD-II adapter in non-debug builds ([VehicleProviderFactory], Phase 3). NFC and Blizzer
 * are still mocks.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        maybeRequestBluetoothPermissions()

        // ---- Hardware layer ---------------------------------------------------------------
        val vehicleProvider = VehicleProviderFactory.create(this)
        val nfcProvider = MockNfcProvider()
        val dataLayerProvider = WearDataLayerBluetoothProvider.getInstance(this)
        val mockPhoneCommunication = MockPhoneCommunication()
        val navPhoneCommunication = BluetoothPhoneCommunication(dataLayerProvider)
        val audioOutput = MockAudioOutput()
        val powerProvider = MockPowerProvider(initial = PowerState.ACTIVE)
        val settingsStore = DataStoreSettingsStore(this)

        // ---- Service / domain layer -----------------------------------------------------------
        val vehicleManager = VehicleDataManager(vehicleProvider)
        val connectionManager = ConnectionManager(nfcProvider, dataLayerProvider)
        val navigationManager = NavigationManager(navPhoneCommunication)
        val navigationAudioManager = NavigationAudioManager(navigationManager, audioOutput)
        // Real now-playing + transport controls over the Data Layer (Phase 1). The phone's
        // MediaNotificationListenerService sends MediaUpdate frames; MediaManager.play()/etc.
        // send MediaCommandMessage frames the phone's WearInboundListenerService acts on.
        val mediaManager = MediaManager(navPhoneCommunication)
        // Real camera-proximity alerts over the Data Layer (Phase 4): the phone's
        // CameraProximityService sends BlizzerTrigger frames as the driver nears a known camera.
        val blizzerManager = BlizzerManager(navPhoneCommunication)
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

        val devControls = DevControlPanel(
            connectionManager,
            nfcProvider,
            VehicleProviderFactory.mockOrNull(vehicleProvider),
            mockPhoneCommunication,
            powerProvider,
        )

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

    /**
     * The BLE OBD adapter link needs BLUETOOTH_SCAN + BLUETOOTH_CONNECT at runtime (API 31+).
     * Debug builds use the mock vehicle provider and don't need these, so only ask when the real
     * one will actually be used.
     */
    private fun maybeRequestBluetoothPermissions() {
        if (BuildConfig.DEBUG) return
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        val needed = arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
        ).filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 1001)
    }
}
