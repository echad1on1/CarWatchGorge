package com.dashboard.wearos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.BlizzerEventType

/**
 * Renders above whichever page [DashboardApp] currently has selected. Deliberately has no
 * parameter for "which panel is underneath" — Blizzer doesn't need to know, and that's the
 * entire point: the same overlay works identically over Car, Maps, or Music because it never
 * looks at what it's covering.
 */
@Composable
fun BlizzerOverlay(event: BlizzerEvent) {
    val backgroundColor = when (event.type) {
        BlizzerEventType.WARNING, BlizzerEventType.ALERT -> Color(0xAAB00020)
        BlizzerEventType.INFO, BlizzerEventType.ANIMATION -> Color(0xAA000000)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(event.message)
    }
}
