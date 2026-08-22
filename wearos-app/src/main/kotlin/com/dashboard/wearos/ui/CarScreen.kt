@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dashboard.wearos.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.domain.Signal
import com.dashboard.core.domain.VehicleData

/**
 * Always available, even before a phone is connected. Renders straight from [VehicleData] —
 * never touches [com.dashboard.core.hardware.VehicleDataProvider] or any hardware directly.
 *
 * Per the spec's car-optimized UI guidance: large text, minimal chrome, the most important
 * values (speed, RPM, gear) get priority placement. [VehicleFieldRow] renders "—" for any
 * [Signal.Unavailable] field rather than a fabricated number — this is the one rule that must
 * never be violated anywhere in this screen.
 */
@Composable
fun CarScreen(
    vehicleData: VehicleData,
    connectionState: ConnectionState,
    onSwipeNext: () -> Unit,
    onOpenDevControls: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .combinedClickable(onClick = onSwipeNext, onLongClick = onOpenDevControls),
        verticalArrangement = Arrangement.Center,
    ) {
        VehicleFieldRow(label = "Speed", signal = vehicleData.speedKmh, unit = "km/h", emphasized = true)
        VehicleFieldRow(label = "RPM", signal = vehicleData.rpm, unit = "")
        VehicleFieldRow(label = "Gear", signal = vehicleData.gear, unit = "")
        VehicleFieldRow(label = "Coolant", signal = vehicleData.coolantTempCelsius, unit = "°C")
        VehicleFieldRow(label = "Oil Pressure", signal = vehicleData.oilPressureKpa, unit = "kPa")

        if (connectionState != ConnectionState.CONNECTED) {
            Text("No phone connected — tap to link")
        }

        // TODO: replace with a real swipe gesture (androidx.wear.compose.foundation
        // SwipeToDismissBox / HorizontalPager) once this is verified in Android Studio.
        // These text affordances exist so the panel-switching logic in DashboardApp is
        // reachable and demoable even before that polish pass.
        Text("swipe → Maps", modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun <T> VehicleFieldRow(label: String, signal: Signal<T>, unit: String, emphasized: Boolean = false) {
    val text = when (signal) {
        is Signal.Available -> "$label: ${signal.value}$unit"
        Signal.Unavailable -> "$label: —" // never fabricate a value the vehicle doesn't expose
    }
    Text(text)
}
