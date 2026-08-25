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
import com.dashboard.core.domain.PowerState
import com.dashboard.core.service.DevControlPanel

/**
 * The one screen with buttons for every developer control described in the spec. Reached via the
 * ⚙ button (debug builds only) in [DashboardApp] — deliberately not part of normal panel
 * swiping, since per spec these controls "exist only for development and will later be removed
 * or hidden."
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

        // Maps: simulates the real strategy — raw spoken-announcement text through the parser,
        // exactly as a phone-side AccessibilityService would capture from Google Maps/Waze.
        "Announce: In 500m turn left" to { devControls.announceNavigation("In 500 meters, turn left") },
        "Announce: In 100m turn left" to { devControls.announceNavigation("In 100 meters, turn left") },
        "Announce: Turn right onto Elm St" to { devControls.announceNavigation("Turn right onto Elm Street") },
        "Announce: Arrived" to { devControls.announceNavigation("You have arrived at your destination") },
        "Stop Navigation" to devControls::stopNavigation,

        "Start Music" to devControls::startMusic,
        "Pause Music" to devControls::pauseMusic,
        "Next Song" to devControls::nextSong,
        "Previous Song" to devControls::previousSong,

        // Blizzer: camera-proximity alerts at decreasing distance, matching the real app.
        // Tap them in sequence to see the overlay's blink speed up as distance shrinks.
        "Camera Warning: 500m" to { devControls.triggerCameraWarning(500) },
        "Camera Warning: 200m" to { devControls.triggerCameraWarning(200) },
        "Camera Warning: 100m" to { devControls.triggerCameraWarning(100) },
        "Dismiss Camera Warning" to devControls::dismissCameraWarning,

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
