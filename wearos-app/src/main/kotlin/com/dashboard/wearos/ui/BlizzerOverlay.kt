package com.dashboard.wearos.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.BlizzerSeverity
import com.dashboard.core.domain.BlizzerSeverityMapper

/**
 * Renders above whichever page DashboardApp currently has selected. Deliberately has no
 * parameter for "which panel is underneath" — Blizzer doesn't need to know, which is why the
 * same overlay works identically over Car, Maps, or Music.
 *
 * Color and blink speed both escalate with proximity, per spec: blue (>1km, calm) -> green
 * (500m-1km) -> red (<=500m, urgent). Severity is computed in `core` (BlizzerSeverityMapper,
 * unit-tested) so this composable only maps a tier to a color, never re-derives the thresholds
 * itself. Auto-dismissal after 5s happens in BlizzerManager (core), not here — this composable
 * just renders whatever is currently active.
 */
@Composable
fun BlizzerOverlay(event: BlizzerEvent) {
    val severity = BlizzerSeverityMapper.forDistance(event.distanceMeters)
    val backgroundColor = when (severity) {
        BlizzerSeverity.DISTANT -> Color(0xFF1565C0)     // blue - far away, calm
        BlizzerSeverity.APPROACHING -> Color(0xFF2E7D32) // green - approaching
        BlizzerSeverity.CLOSE -> Color(0xFFB00020)       // red - close, urgent
        BlizzerSeverity.INFO -> Color(0xFF1A1A2E)        // neutral - non-proximity event
    }

    val blinkPeriodMillis = when (severity) {
        BlizzerSeverity.DISTANT -> 900
        BlizzerSeverity.APPROACHING -> 500
        BlizzerSeverity.CLOSE -> 220
        BlizzerSeverity.INFO -> 900
    }

    val transition = rememberInfiniteTransition(label = "blizzer-blink")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = blinkPeriodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blizzer-blink-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(event.message, fontWeight = FontWeight.Bold)
    }
}
