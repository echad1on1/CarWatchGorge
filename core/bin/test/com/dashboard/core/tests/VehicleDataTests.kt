package com.dashboard.core.tests

import com.dashboard.core.domain.Signal
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertFalse
import com.dashboard.core.testing.assertTrue

fun vehicleDataSuite() = TestSuite("VehicleData / Signal").apply {
    test("Unavailable signal reports isAvailable = false") {
        val signal: Signal<Double> = Signal.Unavailable
        assertFalse(signal.isAvailable, "Unavailable signal should not report available")
    }

    test("Available signal reports isAvailable = true and carries its value") {
        val signal = Signal.Available(42.0, 12345L)
        assertTrue(signal.isAvailable, "Available signal should report available")
        assertEquals(42.0, signal.value, "value should round-trip")
    }

    test("orElse returns fallback for Unavailable, never a fabricated real reading") {
        val signal: Signal<Double> = Signal.Unavailable
        assertEquals(-1.0, signal.orElse(-1.0), "orElse should only ever return the caller-provided fallback")
    }

    test("orElse returns the real value when available, ignoring the fallback") {
        val signal: Signal<Double> = Signal.Available(99.5, 1L)
        assertEquals(99.5, signal.orElse(-1.0), "orElse should prefer the real value")
    }

    test("a VehicleData with only some Signals is legal (partial vehicle support)") {
        val data = com.dashboard.core.domain.VehicleData(
            speedKmh = Signal.Available(60.0, 1L),
            oilPressureKpa = Signal.Unavailable,
        )
        assertTrue(data.speedKmh.isAvailable, "speed should be available")
        assertFalse(data.oilPressureKpa.isAvailable, "oil pressure should be unavailable, not defaulted")
    }
}
