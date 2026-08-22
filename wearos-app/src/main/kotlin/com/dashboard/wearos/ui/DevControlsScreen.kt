package com.dashboard.wearos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.dashboard.core.domain.BlizzerEventType
import com.dashboard.core.domain.Direction
import com.dashboard.core.domain.PowerState
import com.dashboard.core.service.DevControlPanel

/**
 * The one screen with buttons for every developer control described in the spec (Simulate NFC
 * Tap, Connect/Disconnect Phone, Start/Stop Navigation, Change Direction, music transport,
 * Trigger Blizzer). Reached via long-press from [CarScreen] — deliberately not part of normal
 * panel swiping, since per spec these controls "exist only for development and will later be
 * removed or hidden."
 *
 * This screen has NO logic of its own — every button is a direct call into [DevControlPanel].
 * Removing this screen when shipping to real hardware should never require touching
 * [DevControlPanel] or anything below it.
 */
@Composable
fun DevControlsScreen(devControls: DevControlPanel, onClose: () -> Unit) {
    val actions: List<Pair<String, () -> Unit>> = listOf(
        "Close" to onClose,
        "Simulate NFC Tap" to devControls::simulateNfcTap,
        "Disconnect Phone" to devControls::disconnectPhone,
        "Drive: 0 km/h" to { devControls.setTargetSpeedKmh(0.0) },
        "Drive: 60 km/h" to { devControls.setTargetSpeedKmh(60.0) },
        "Drive: 120 km/h" to { devControls.setTargetSpeedKmh(120.0) },
        "Start Navigation" to { devControls.startNavigation() },
        "Stop Navigation" to devControls::stopNavigation,
        "Direction: Left" to { devControls.changeDirection(Direction.TURN_LEFT, 100.0) },
        "Direction: Right" to { devControls.changeDirection(Direction.TURN_RIGHT, 100.0) },
        "Start Music" to devControls::startMusic,
        "Pause Music" to devControls::pauseMusic,
        "Next Song" to devControls::nextSong,
        "Previous Song" to devControls::previousSong,
        "Trigger Blizzer (Info)" to { devControls.triggerBlizzer("Welcome back!", BlizzerEventType.INFO) },
        "Trigger Blizzer (Warning)" to { devControls.triggerBlizzer("Check engine soon", BlizzerEventType.WARNING) },
        "Simulate car SLEEP" to { devControls.simulatePowerState(PowerState.SLEEP) },
        "Simulate car ACTIVE" to { devControls.simulatePowerState(PowerState.ACTIVE) },
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(actions) { (label, action) ->
            Button(onClick = action) { Text(label) }
        }
    }
}
