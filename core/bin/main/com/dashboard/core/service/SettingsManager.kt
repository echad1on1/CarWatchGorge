package com.dashboard.core.service

import com.dashboard.core.domain.CarPanelSettings
import com.dashboard.core.domain.DashboardSettings
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.SettingsStore
import com.dashboard.core.hardware.Subscription

/**
 * The only way the rest of the app reads or changes [DashboardSettings]. Panels observe
 * [current]/[observe] to react to changes (e.g. Car panel re-rendering when the user reorders
 * visible fields) without ever touching [SettingsStore] directly.
 */
class SettingsManager(private val store: SettingsStore) {

    private val emitter = Emitter<DashboardSettings>()

    var current: DashboardSettings = store.load()
        private set

    fun observe(listener: (DashboardSettings) -> Unit): Subscription {
        listener(current)
        return emitter.subscribe(listener)
    }

    fun update(settings: DashboardSettings) {
        current = settings
        store.save(settings)
        emitter.emit(settings)
    }

    fun updateCarPanel(transform: (CarPanelSettings) -> CarPanelSettings) {
        update(current.copy(carPanel = transform(current.carPanel)))
    }
}
