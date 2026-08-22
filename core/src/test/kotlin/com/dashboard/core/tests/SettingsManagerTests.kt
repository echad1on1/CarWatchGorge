package com.dashboard.core.tests

import com.dashboard.core.domain.CarLayoutPreset
import com.dashboard.core.domain.VehicleFieldKeys
import com.dashboard.core.hardware.mock.InMemorySettingsStore
import com.dashboard.core.service.SettingsManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals

fun settingsManagerSuite() = TestSuite("SettingsManager").apply {

    test("starts with defaults from the store") {
        val manager = SettingsManager(InMemorySettingsStore())
        assertEquals(CarLayoutPreset.MINIMAL, manager.current.carPanel.preset, "should load the default preset")
    }

    test("update() persists through the store and notifies observers") {
        val store = InMemorySettingsStore()
        val manager = SettingsManager(store)
        var observedPreset: CarLayoutPreset? = null
        manager.observe { observedPreset = it.carPanel.preset }

        manager.updateCarPanel { it.copy(preset = CarLayoutPreset.PERFORMANCE) }

        assertEquals(CarLayoutPreset.PERFORMANCE, manager.current.carPanel.preset, "current should reflect update")
        assertEquals(CarLayoutPreset.PERFORMANCE, observedPreset, "observer should be notified")
        assertEquals(CarLayoutPreset.PERFORMANCE, store.load().carPanel.preset, "store should have persisted it")
    }

    test("a fresh SettingsManager over the same store sees prior updates") {
        val store = InMemorySettingsStore()
        val first = SettingsManager(store)
        first.updateCarPanel { it.copy(visibleFields = listOf(VehicleFieldKeys.SPEED)) }

        val second = SettingsManager(store)
        assertEquals(listOf(VehicleFieldKeys.SPEED), second.current.carPanel.visibleFields, "should see persisted change")
    }
}
