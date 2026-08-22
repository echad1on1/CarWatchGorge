package com.dashboard.wearos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.domain.NavigationState
import com.dashboard.core.domain.VehicleData
import com.dashboard.core.service.BlizzerManager
import com.dashboard.core.service.ConnectionManager
import com.dashboard.core.service.DevControlPanel
import com.dashboard.core.service.MediaManager
import com.dashboard.core.service.NavigationManager
import com.dashboard.core.service.SettingsManager
import com.dashboard.core.service.VehicleDataManager
import com.dashboard.wearos.ui.BlizzerOverlay
import com.dashboard.wearos.ui.CarScreen
import com.dashboard.wearos.ui.DevControlsScreen
import com.dashboard.wearos.ui.MapsScreen
import com.dashboard.wearos.ui.MusicScreen
import com.dashboard.wearos.ui.Panel

/**
 * Top-level UI. Owns which [Panel] is showing and layers [BlizzerOverlay] on top of whichever
 * one that is — this is the concrete implementation of "Blizzer overlays any active screen and
 * returns to that exact screen afterward": [selectedPanel] never changes when Blizzer fires, so
 * whatever was showing underneath is exactly what's showing once [BlizzerManager.currentEvent]
 * goes back to null.
 *
 * Car is always reachable. Maps/Music only appear as options once [ConnectionManager] reports
 * [ConnectionState.CONNECTED] — this mirrors [com.dashboard.core.domain.PanelAvailability]
 * exactly, rather than re-deriving the same rule in the UI.
 */
@Composable
fun DashboardApp(
    vehicleManager: VehicleDataManager,
    connectionManager: ConnectionManager,
    navigationManager: NavigationManager,
    mediaManager: MediaManager,
    blizzerManager: BlizzerManager,
    settingsManager: SettingsManager,
    devControls: DevControlPanel,
) {
    val connectionState by observeAsState(connectionManager.state) { connectionManager.observeState(it) }
    val vehicleData by observeAsState(VehicleData()) { vehicleManager.observe(it) }
    val navigationState by observeAsState(NavigationState.INACTIVE) { navigationManager.observe(it) }
    val mediaState by observeAsState(mediaManager.latest) { mediaManager.observe(it) }
    val blizzerEvent by observeAsState(blizzerManager.currentEvent) { blizzerManager.observe(it) }

    var selectedPanel by remember { mutableStateOf(Panel.CAR) }
    val isConnected = connectionState == ConnectionState.CONNECTED

    // If the phone disconnects while Maps/Music was showing, fall back to Car — matches the
    // spec's "when the phone disconnects the system returns to the normal Car panel."
    if (!isConnected && selectedPanel != Panel.CAR && selectedPanel != Panel.DEV_CONTROLS) {
        selectedPanel = Panel.CAR
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedPanel) {
            Panel.CAR -> CarScreen(
                vehicleData = vehicleData,
                connectionState = connectionState,
                onSwipeNext = { if (isConnected) selectedPanel = Panel.MAPS },
                onOpenDevControls = { selectedPanel = Panel.DEV_CONTROLS },
            )
            Panel.MAPS -> MapsScreen(
                navigationState = navigationState,
                onSwipePrevious = { selectedPanel = Panel.CAR },
                onSwipeNext = { selectedPanel = Panel.MUSIC },
            )
            Panel.MUSIC -> MusicScreen(
                mediaState = mediaState,
                onPlay = mediaManager::play,
                onPause = mediaManager::pause,
                onNext = mediaManager::next,
                onPrevious = mediaManager::previous,
                onSwipePrevious = { selectedPanel = Panel.MAPS },
            )
            Panel.DEV_CONTROLS -> DevControlsScreen(
                devControls = devControls,
                onClose = { selectedPanel = Panel.CAR },
            )
        }

        // Blizzer renders above whatever panel is selected, and only that — it never
        // participates in panel selection itself.
        blizzerEvent?.let { event -> BlizzerOverlay(event = event) }
    }
}
