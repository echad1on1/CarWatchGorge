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
import com.dashboard.core.domain.BlizzerProximity

/**
 * Renders above whichever page [DashboardApp] currently has selected. Deliberately has no
 * parameter for "which panel is underneath" — Blizzer doesn't need to know, and that's the
 * entire point: the same overlay works identically over Car, Maps, or Music because it never
 * looks at what it's covering.
 *
 * Blizzer is a speed-camera proximity alert (see [BlizzerEvent.distanceMeters] doc). Color and
 * blink speed escalate as the camera gets closer — blue at long range through green, amber, and
 * red right before it. Non-proximity events (no distance) use a neutral background.
 */
@Composable
fun BlizzerOverlay(event: BlizzerEvent) {
    val backgroundColor = when {
        event.distanceMeters != null -> Color(BlizzerProximity.colorArgbFor(event.distanceMeters))
        event.type == BlizzerEventType.WARNING || event.type == BlizzerEventType.ALERT -> Color(0xFFB00020)
        else -> Color(BlizzerProximity.COLOR_NEUTRAL)
    }

    val blinkPeriodMillis = BlizzerProximity.blinkPeriodMillisFor(event.distanceMeters)

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
