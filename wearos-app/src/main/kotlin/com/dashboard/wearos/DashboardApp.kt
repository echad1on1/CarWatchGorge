package com.dashboard.wearos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Text
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
import kotlinx.coroutines.delay

/**
 * Top-level UI. Real swipe navigation via [HorizontalPager] — pages are Car-only before a phone
 * is connected, and Car/Maps/Music once [ConnectionState.CONNECTED]. [BlizzerOverlay] renders
 * on top of the pager, not as one of its pages, so it never changes which page is selected —
 * that's the concrete mechanism behind "Blizzer overlays whatever screen is active and returns
 * to that exact screen afterward."
 *
 * The developer-controls entry point (top-right gear/build icon) only renders in debug builds
 * ([BuildConfig.DEBUG]) — per spec, these controls "exist only for development and will later
 * be removed or hidden," and gating on the build type is how that removal actually happens for
 * a release build, rather than relying on someone remembering to delete a button.
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

    var showDevControls by remember { mutableStateOf(false) }

    val isConnected = connectionState == ConnectionState.CONNECTED
    // Page 0 is always Car. Pages 1/2 (Maps/Music) only exist once connected — matches
    // com.dashboard.core.domain.PanelAvailability exactly, rather than re-deriving the rule here.
    val pageCount = if (isConnected) 3 else 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // "When the phone disconnects the system returns to the normal Car panel" (spec) —
    // concretely: snap the pager back to page 0 the moment connection drops. Wrapped in
    // LaunchedEffect(isConnected) rather than called directly in the composable body, so the
    // scroll animation only launches once per actual disconnect, not on every recomposition.
    LaunchedEffect(isConnected) {
        if (!isConnected && pagerState.currentPage != 0) {
            pagerState.animateScrollToPage(0)
        }
    }

    // Auto-dismiss Blizzer after 5 seconds, returning to whichever panel was underneath.
    LaunchedEffect(blizzerEvent?.id, blizzerEvent?.timestampMillis) {
        val event = blizzerEvent ?: return@LaunchedEffect
        delay(5_000)
        if (blizzerManager.currentEvent?.id == event.id) {
            blizzerManager.dismissCurrentEvent()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showDevControls) {
            DevControlsScreen(devControls = devControls, onClose = { showDevControls = false })
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> CarScreen(vehicleData = vehicleData, connectionState = connectionState)
                    1 -> MapsScreen(navigationState = navigationState)
                    2 -> MusicScreen(
                        mediaState = mediaState,
                        onPlay = mediaManager::play,
                        onPause = mediaManager::pause,
                        onNext = mediaManager::next,
                        onPrevious = mediaManager::previous,
                    )
                }
            }

            if (pageCount > 1) {
                PageIndicator(pageCount = pageCount, currentPage = pagerState.currentPage)
            }

            if (BuildConfig.DEBUG) {
                // TopCenter, not TopEnd: a round watch bezel clips corners far more aggressively
                // than the top-center — a button placed in a corner (as a first pass of this
                // screen did) can end up unreachable/invisible depending on the exact device shape.
                IconButton(
                    onClick = { showDevControls = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                ) {
                    Text("⚙")
                }
            }
        }

        // Blizzer renders above everything, including the dev-controls screen, and never
        // participates in page/screen selection itself.
        blizzerEvent?.let { event -> BlizzerOverlay(event = event) }
    }
}

@Composable
private fun BoxScope.PageIndicator(pageCount: Int, currentPage: Int) {
    // Deliberately simple dots rather than androidx.wear.compose.material3's HorizontalPageIndicator,
    // to keep this file's API surface small and easy to verify by eye until this is build-tested
    // in Android Studio — swap in the Wear-specific component there if desired.
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(if (index == currentPage) Color.White else Color.Gray),
            )
        }
    }
}
