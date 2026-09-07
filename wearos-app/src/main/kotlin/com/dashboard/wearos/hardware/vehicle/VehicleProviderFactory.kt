package com.dashboard.wearos.hardware.vehicle

import android.content.Context
import com.dashboard.core.hardware.VehicleDataProvider
import com.dashboard.core.hardware.mock.MockVehicleDataProvider
import com.dashboard.wearos.BuildConfig

/**
 * Chooses the Car panel's data source. Debug builds keep [MockVehicleDataProvider] so the
 * dev-controls "drive" simulation and the `TESTING.md` emulator walkthrough still work with no
 * hardware; every other build uses the real [BleObdVehicleDataProvider].
 *
 * [mockOrNull] gives `MainActivity` the concrete mock (or null) that `DevControlPanel` needs
 * without it having to know which provider was chosen.
 */
object VehicleProviderFactory {

    fun create(context: Context): VehicleDataProvider =
        if (BuildConfig.DEBUG) MockVehicleDataProvider() else BleObdVehicleDataProvider(context)

    fun mockOrNull(provider: VehicleDataProvider): MockVehicleDataProvider? =
        provider as? MockVehicleDataProvider
}
