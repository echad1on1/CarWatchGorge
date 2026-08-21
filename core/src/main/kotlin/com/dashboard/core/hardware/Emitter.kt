package com.dashboard.core.hardware

/**
 * Minimal multi-listener emitter. Deliberately dependency-free (no coroutines/Flow, no RxJava)
 * so the `core` module has zero external dependencies and stays portable to whatever the final
 * UI toolkit turns out to be. When the Wear OS/Compose UI is added, this can be wrapped in a
 * StateFlow adapter at the UI boundary without changing anything in `core`.
 */
class Emitter<T> {
    private val listeners = mutableListOf<(T) -> Unit>()

    fun subscribe(listener: (T) -> Unit): Subscription {
        listeners.add(listener)
        return Subscription { listeners.remove(listener) }
    }

    fun emit(value: T) {
        // Copy to avoid ConcurrentModificationException if a listener subscribes/unsubscribes mid-emit.
        listeners.toList().forEach { it(value) }
    }
}
