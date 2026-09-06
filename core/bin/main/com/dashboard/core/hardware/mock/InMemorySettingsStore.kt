package com.dashboard.core.hardware.mock

import com.dashboard.core.domain.DashboardSettings
import com.dashboard.core.hardware.SettingsStore

/**
 * Development stand-in for persisted settings. Lives only in memory for the lifetime of the
 * process — a real implementation (Android DataStore, a file, etc.) will be dropped in later
 * behind the same [SettingsStore] interface.
 */
class InMemorySettingsStore(initial: DashboardSettings = DashboardSettings()) : SettingsStore {
    private var current = initial

    override fun load(): DashboardSettings = current
    override fun save(settings: DashboardSettings) {
        current = settings
    }
}
