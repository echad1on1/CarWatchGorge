package com.dashboard.core.tests

import com.dashboard.core.domain.CarLayoutPreset
import com.dashboard.core.domain.CarPanelSettings
import com.dashboard.core.domain.DashboardSettings
import com.dashboard.core.domain.SettingsCodec
import com.dashboard.core.domain.VehicleFieldKeys
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals

fun settingsCodecSuite() = TestSuite("SettingsCodec").apply {

    test("round-trips defaults") {
        val original = DashboardSettings()
        val decoded = SettingsCodec.decode(SettingsCodec.encode(original))
        assertEquals(original, decoded, "encode then decode is identity for defaults")
    }

    test("round-trips a custom layout") {
        val original = DashboardSettings(
            carPanel = CarPanelSettings(
                preset = CarLayoutPreset.CUSTOM,
                visibleFields = listOf(VehicleFieldKeys.SPEED, VehicleFieldKeys.RPM, VehicleFieldKeys.BATTERY_VOLTAGE),
            ),
        )
        val decoded = SettingsCodec.decode(SettingsCodec.encode(original))
        assertEquals(original, decoded, "custom preset + field list survives")
    }

    test("unknown keys are ignored, missing keys fall back to defaults") {
        val decoded = SettingsCodec.decode("carPanel.preset=PERFORMANCE\nfuture.setting=whatever")
        assertEquals(CarLayoutPreset.PERFORMANCE, decoded.carPanel.preset, "known key read")
        assertEquals(DashboardSettings().carPanel.visibleFields, decoded.carPanel.visibleFields, "missing key -> default")
    }

    test("a garbage preset value falls back to the default, not a crash") {
        val decoded = SettingsCodec.decode("carPanel.preset=NONSENSE")
        assertEquals(DashboardSettings().carPanel.preset, decoded.carPanel.preset, "bad enum -> default")
    }

    test("empty string decodes to defaults") {
        assertEquals(DashboardSettings(), SettingsCodec.decode(""), "empty -> defaults")
    }
}
