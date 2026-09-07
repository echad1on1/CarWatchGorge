package com.dashboard.wearos.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.PlaybackState
import kotlin.math.abs
import kotlin.math.sin

/**
 * Displays whatever [MediaState] the phone reports and turns taps into
 * [com.dashboard.core.service.MediaManager] commands. No dependency on any specific music app —
 * `title`/`artist`/`playbackState` are all this screen ever looks at. Page 2 in
 * [com.dashboard.wearos.DashboardApp]'s pager, only reachable once connected.
 *
 * The bar "waveform" is purely decorative — audio never plays on the watch, so there is no real
 * signal to analyse. It advances only while [PlaybackState.PLAYING] and is seeded by
 * `positionMillis` so it roughly tracks the song, the same "interpolate between discrete phone
 * updates" idea `NavigationManager` uses for the turn countdown.
 */
@Composable
fun MusicScreen(
    mediaState: MediaState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val title = mediaState.title // local val avoids the cross-module smart-cast restriction
        if (title == null) {
            Text("Nothing playing")
        } else {
            Text(title, maxLines = 1)
            mediaState.artist?.let { Text(it, maxLines = 1) }

            val playing = mediaState.playbackState == PlaybackState.PLAYING
            Waveform(
                playing = playing,
                positionMillis = mediaState.positionMillis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(vertical = 6.dp),
            )
            ProgressBar(
                positionMillis = mediaState.positionMillis,
                durationMillis = mediaState.durationMillis,
                modifier = Modifier.fillMaxWidth().height(3.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onPrevious) { Text("⏮") }
                if (playing) {
                    Button(onClick = onPause) { Text("⏸") }
                } else {
                    Button(onClick = onPlay) { Text("▶") }
                }
                Button(onClick = onNext) { Text("⏭") }
            }
        }
    }
}

@Composable
private fun Waveform(playing: Boolean, positionMillis: Long, modifier: Modifier) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) phase += (now - last) / 1_000_000_000f * 4f
                last = now
            }
        }
    }
    val bars = 24
    val seed = positionMillis / 200f
    Canvas(modifier = modifier) {
        val gap = size.width / bars
        val barWidth = gap * 0.55f
        for (i in 0 until bars) {
            val wave = sin(seed + phase + i * 0.7f) * 0.5f + sin(phase * 0.5f + i * 1.9f) * 0.3f
            val amp = if (playing) 0.25f + abs(wave) * 0.75f else 0.15f
            val barHeight = size.height * amp
            drawRect(
                color = Color(0xFF4CAF50),
                topLeft = androidx.compose.ui.geometry.Offset(i * gap, (size.height - barHeight) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )
        }
    }
}

@Composable
private fun ProgressBar(positionMillis: Long, durationMillis: Long, modifier: Modifier) {
    val fraction = if (durationMillis > 0) (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(fraction, tween(300, easing = LinearEasing), label = "media-progress")
    Canvas(modifier = modifier) {
        drawRect(Color(0x33FFFFFF), size = size)
        drawRect(
            Color(0xFF4CAF50),
            size = androidx.compose.ui.geometry.Size(size.width * animated, size.height),
        )
    }
}
