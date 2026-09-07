package com.dashboard.wearos.hardware

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dashboard.core.domain.DashboardSettings
import com.dashboard.core.domain.SettingsCodec
import com.dashboard.core.hardware.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dashboard_settings")
private val SETTINGS_KEY = stringPreferencesKey("settings_v1")

/**
 * Real [SettingsStore] for the watch, backed by Jetpack DataStore. `core`'s interface is
 * synchronous and DataStore is not, so the async-ness is absorbed here:
 *  - [load] does exactly one blocking read at startup (then serves an in-memory mirror);
 *  - [save] is fire-and-forget on an IO scope.
 *
 * If that single startup block ever proves unacceptable, the fallback is a `suspend
 * loadInitial()` on the interface — a small `core` change, flagged in PLAN.md, not expected.
 */
class DataStoreSettingsStore(context: Context) : SettingsStore {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var mirror: DashboardSettings? = null

    override fun load(): DashboardSettings {
        mirror?.let { return it }
        val loaded = runCatching {
            runBlocking {
                appContext.settingsDataStore.data.first()[SETTINGS_KEY]
                    ?.let { SettingsCodec.decode(it) }
                    ?: DashboardSettings()
            }
        }.getOrDefault(DashboardSettings())
        mirror = loaded
        return loaded
    }

    override fun save(settings: DashboardSettings) {
        mirror = settings
        scope.launch {
            runCatching {
                appContext.settingsDataStore.edit { it[SETTINGS_KEY] = SettingsCodec.encode(settings) }
            }
        }
    }
}
