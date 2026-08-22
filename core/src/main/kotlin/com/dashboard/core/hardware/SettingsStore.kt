package com.dashboard.core.hardware

import com.dashboard.core.domain.DashboardSettings

/**
 * Abstracts wherever [DashboardSettings] actually lives. Today, nothing is persisted between
 * runs (see [com.dashboard.core.hardware.mock.InMemorySettingsStore]); on the real device this
 * will be backed by on-disk storage (e.g. Android DataStore). Nothing above this interface
 * should know or care which.
 */
interface SettingsStore {
    fun load(): DashboardSettings
    fun save(settings: DashboardSettings)
}
