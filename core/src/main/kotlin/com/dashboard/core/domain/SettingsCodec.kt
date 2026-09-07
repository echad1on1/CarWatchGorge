package com.dashboard.core.domain

/**
 * Pure `DashboardSettings` ↔ `String` serialization. Kept in `core` so the format is
 * unit-tested and reused by any transport (the `ProtocolMessage.SettingsUpdate` placeholder)
 * as well as by the watch's on-disk `SettingsStore`. Deliberately a tiny line format, not
 * JSON — no dependency, and forward-compatible: unknown keys are ignored, missing keys fall
 * back to the [DashboardSettings] defaults.
 *
 * ```
 * carPanel.preset=CUSTOM
 * carPanel.visibleFields=speed,rpm,gear
 * ```
 */
object SettingsCodec {

    fun encode(settings: DashboardSettings): String = buildString {
        appendLine("carPanel.preset=${settings.carPanel.preset.name}")
        appendLine("carPanel.visibleFields=${settings.carPanel.visibleFields.joinToString(",")}")
    }.trimEnd('\n')

    fun decode(text: String): DashboardSettings {
        val fields = text.lineSequence()
            .mapNotNull { line ->
                val i = line.indexOf('=')
                if (i <= 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
            }
            .toMap()

        val default = DashboardSettings()

        val preset = fields["carPanel.preset"]
            ?.let { runCatching { CarLayoutPreset.valueOf(it) }.getOrNull() }
            ?: default.carPanel.preset

        val visibleFields = fields["carPanel.visibleFields"]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: default.carPanel.visibleFields

        return DashboardSettings(
            carPanel = CarPanelSettings(preset = preset, visibleFields = visibleFields),
        )
    }
}
