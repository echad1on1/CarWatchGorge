package com.dashboard.wearos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.domain.Gear
import com.dashboard.core.domain.GearPosition
import com.dashboard.core.domain.Signal
import com.dashboard.core.domain.VehicleData

/**
 * Always available, even before a phone is connected. Renders straight from [VehicleData] —
 * never touches [com.dashboard.core.hardware.VehicleDataProvider] or any hardware directly.
 * Page 0 in [com.dashboard.wearos.DashboardApp]'s pager; swiping to Maps/Music (once connected)
 * and reaching developer controls are both handled there, not in this screen.
 *
 * Uses generous padding and centered, single-line-friendly text specifically because Wear OS
 * screens are round — content placed close to the edge (as a first pass of this screen did) gets
 * clipped by the bezel. [VehicleFieldRow] renders "—" for any [Signal.Unavailable] field rather
 * than a fabricated number — this is the one rule that must never be violated anywhere here.
 */
@Composable
fun CarScreen(
    vehicleData: VehicleData,
    connectionState: ConnectionState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp), // generous inset so text isn't clipped by the round bezel
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        VehicleFieldRow(label = "Speed", value = vehicleData.speedKmh.formatDouble(), unit = "km/h")
        VehicleFieldRow(label = "RPM", value = vehicleData.rpm.formatInt(), unit = "")
        VehicleFieldRow(label = "Gear", value = vehicleData.gear.formatGear(), unit = "")
        VehicleFieldRow(label = "Coolant", value = vehicleData.coolantTempCelsius.formatDouble(), unit = "°C")
        VehicleFieldRow(label = "Oil", value = vehicleData.oilPressureKpa.formatDouble(), unit = "kPa")

        if (connectionState != ConnectionState.CONNECTED) {
            Text(
                text = "No phone connected",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun VehicleFieldRow(label: String, value: String, unit: String) {
    Text(
        text = "$label: $value$unit",
        fontSize = 14.sp,
    )
}

// --- Display formatting. Kept here (not in `core`) since it's presentation-only, and every
// path still renders "—" for Signal.Unavailable rather than ever fabricating a value. ---

private fun Signal<Double>.formatDouble(): String = when (this) {
    is Signal.Available -> String.format("%.1f", value)
    Signal.Unavailable -> "—"
}

private fun Signal<Int>.formatInt(): String = when (this) {
    is Signal.Available -> value.toString()
    Signal.Unavailable -> "—"
}

private fun Signal<Gear>.formatGear(): String = when (this) {
    is Signal.Available -> {
        val g = value
        when (g.position) {
            GearPosition.PARK -> "P"
            GearPosition.REVERSE -> "R"
            GearPosition.NEUTRAL -> "N"
            GearPosition.DRIVE -> "D${g.numberedGear ?: ""}"
            GearPosition.NUMBERED -> "${g.numberedGear ?: "?"}"
            GearPosition.UNKNOWN -> "?"
        }
    }
    Signal.Unavailable -> "—"
}
