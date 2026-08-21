package com.dashboard.core.tests

import com.dashboard.core.domain.Signal
import com.dashboard.core.domain.VehicleData
import com.dashboard.core.service.VehicleDataManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertFalse
import com.dashboard.core.testing.assertTrue

fun vehicleDataManagerSuite() = TestSuite("VehicleDataManager").apply {

    test("late subscriber immediately receives the latest known snapshot") {
        val provider = FakeVehicleDataProvider()
        val manager = VehicleDataManager(provider)
        manager.start()
        provider.push(VehicleData(speedKmh = Signal.Available(50.0, 1L)))

        var received: VehicleData? = null
        manager.observe { received = it }

        assertEquals(50.0, (received!!.speedKmh as Signal.Available).value, "late subscriber should get cached snapshot")
    }

    test("subscriber receives subsequent updates as they arrive") {
        val provider = FakeVehicleDataProvider()
        val manager = VehicleDataManager(provider)
        manager.start()
        // Manager delivers the initial (Unavailable) snapshot immediately on subscribe; this
        // suite only cares about *available* speed readings, so the initial one is skipped here.
        val speedsSeen = mutableListOf<Double>()
        manager.observe { data ->
            (data.speedKmh as? Signal.Available)?.let { speedsSeen.add(it.value) }
        }

        provider.push(VehicleData(speedKmh = Signal.Available(10.0, 1L)))
        provider.push(VehicleData(speedKmh = Signal.Available(20.0, 2L)))

        assertEquals(2, speedsSeen.size, "should have seen exactly the 2 pushed updates")
        assertEquals(20.0, speedsSeen.last(), "last update should be latest push")
    }

    test("unavailable field stays unavailable through the manager, never fabricated") {
        val provider = FakeVehicleDataProvider()
        val manager = VehicleDataManager(provider)
        manager.start()
        provider.push(VehicleData(oilPressureKpa = Signal.Unavailable))

        var received: VehicleData? = null
        manager.observe { received = it }

        assertFalse(received!!.oilPressureKpa.isAvailable, "oil pressure should remain Unavailable end-to-end")
        assertTrue(true, "sanity")
    }

    test("start() starts the underlying provider") {
        val provider = FakeVehicleDataProvider()
        val manager = VehicleDataManager(provider)
        assertFalse(provider.started, "should not be started before manager.start()")
        manager.start()
        assertTrue(provider.started, "manager.start() should start the provider")
    }
}
