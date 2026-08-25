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
import com.dashboard.core.domain.BlizzerEventType

/**
 * Renders above whichever page [DashboardApp] currently has selected. Deliberately has no
 * parameter for "which panel is underneath" — Blizzer doesn't need to know, and that's the
 * entire point: the same overlay works identically over Car, Maps, or Music because it never
 * looks at what it's covering.
 *
 * Blizzer is a speed-camera proximity alert (see [BlizzerEvent.distanceMeters] doc). The real
 * app beeps as the camera gets closer; here the visual analog is a blink that speeds up the
 * closer the camera is, escalating from a slow pulse at long range to a fast, urgent flash right
 * before it. [BlizzerAudioManager][com.dashboard.core.service.BlizzerAudioManager] handles the
 * actual beep — this composable is purely the visual side of the same event.
 */
@Composable
fun BlizzerOverlay(event: BlizzerEvent) {
    val backgroundColor = when (event.type) {
        BlizzerEventType.WARNING, BlizzerEventType.ALERT -> Color(0xFFB00020)
        BlizzerEventType.INFO, BlizzerEventType.ANIMATION -> Color(0xFF1A1A2E)
    }

    val blinkPeriodMillis = blinkPeriodFor(event.distanceMeters)

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

/**
 * Closer camera -> faster blink. No distance (a generic, non-proximity Blizzer event) blinks at
 * a calm, unhurried default rate.
 */
private fun blinkPeriodFor(distanceMeters: Int?): Int = when {
    distanceMeters == null -> 900
    distanceMeters <= 100 -> 220
    distanceMeters <= 200 -> 400
    distanceMeters <= 500 -> 650
    else -> 900
}
