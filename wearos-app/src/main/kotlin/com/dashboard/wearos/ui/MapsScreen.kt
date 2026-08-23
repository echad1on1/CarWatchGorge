package com.dashboard.wearos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.dashboard.core.domain.Direction
import com.dashboard.core.domain.NavigationState

/**
 * Only ever renders what [NavigationState] says — no destination search, no route calculation,
 * no knowledge of how navigation data got here. When `active == false`, explicitly shows
 * "navigation not running" per spec, never stale data from a previous session. Page 1 in
 * [com.dashboard.wearos.DashboardApp]'s pager, only reachable once connected.
 */
@Composable
fun MapsScreen(navigationState: NavigationState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (!navigationState.active) {
            Text("Navigation not running")
        } else {
            Text(directionArrow(navigationState.direction), fontSize = 48.sp, fontWeight = FontWeight.Bold)
            navigationState.distanceMeters?.let { Text("${it.toInt()} m") }
            navigationState.roadName?.let { Text(it) }
            navigationState.etaMinutes?.let { Text("ETA ${it} min") }
        }
    }
}

private fun directionArrow(direction: Direction): String = when (direction) {
    Direction.STRAIGHT -> "↑"
    Direction.TURN_LEFT -> "←"
    Direction.TURN_RIGHT -> "→"
    Direction.KEEP_LEFT -> "↖"
    Direction.KEEP_RIGHT -> "↗"
    Direction.ROUNDABOUT -> "↻"
    Direction.ARRIVED -> "●"
    Direction.UNKNOWN -> "?"
}
