package com.dashboard.wearos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Bridges any `core` manager's `observe(listener) -> Subscription` callback API into Compose
 * [State], without `core` needing any dependency on coroutines/Flow. `core` was deliberately
 * kept dependency-free (see core/build.gradle.kts and [com.dashboard.core.hardware.Emitter]'s
 * class doc) — this is the one place that trade-off is paid for, and it's paid here in the UI
 * module rather than in `core` itself.
 *
 * Usage: `val vehicleData by observeAsState(initial) { manager.observe(it) }`
 */
@Composable
fun <T> observeAsState(initial: T, subscribe: ((T) -> Unit) -> com.dashboard.core.hardware.Subscription): State<T> {
    val state = remember { mutableStateOf(initial) }
    DisposableEffect(Unit) {
        val subscription = subscribe { value -> state.value = value }
        onDispose { subscription.cancel() }
    }
    return state
}
