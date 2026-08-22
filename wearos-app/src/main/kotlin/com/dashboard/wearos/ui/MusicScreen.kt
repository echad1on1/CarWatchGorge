package com.dashboard.wearos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.PlaybackState

/**
 * Displays whatever [MediaState] the phone reports and turns taps into
 * [com.dashboard.core.service.MediaManager] commands. No dependency on any specific music app —
 * `title`/`artist`/`playbackState` are all this screen ever looks at.
 */
@Composable
fun MusicScreen(
    mediaState: MediaState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSwipePrevious: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (mediaState.title == null) {
            Text("Nothing playing")
        } else {
            Text(mediaState.title)
            mediaState.artist?.let { Text(it) }

            Row {
                Button(onClick = onPrevious) { Text("⏮") }
                if (mediaState.playbackState == PlaybackState.PLAYING) {
                    Button(onClick = onPause) { Text("⏸") }
                } else {
                    Button(onClick = onPlay) { Text("▶") }
                }
                Button(onClick = onNext) { Text("⏭") }
            }
        }
        Text("← Maps", modifier = Modifier.padding(top = 8.dp).clickable(onClick = onSwipePrevious))
    }
}
