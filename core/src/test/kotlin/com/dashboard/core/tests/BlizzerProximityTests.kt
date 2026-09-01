package com.dashboard.core.tests

import com.dashboard.core.domain.BlizzerProximity
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals

fun blizzerProximitySuite() = TestSuite("BlizzerProximity").apply {

    test("colorArgbFor maps distance bands blue → green → amber → red") {
        assertEquals(BlizzerProximity.COLOR_BLUE, BlizzerProximity.colorArgbFor(2000))
        assertEquals(BlizzerProximity.COLOR_BLUE, BlizzerProximity.colorArgbFor(1500))
        assertEquals(BlizzerProximity.COLOR_GREEN, BlizzerProximity.colorArgbFor(1000))
        assertEquals(BlizzerProximity.COLOR_GREEN, BlizzerProximity.colorArgbFor(750))
        assertEquals(BlizzerProximity.COLOR_AMBER, BlizzerProximity.colorArgbFor(500))
        assertEquals(BlizzerProximity.COLOR_AMBER, BlizzerProximity.colorArgbFor(300))
        assertEquals(BlizzerProximity.COLOR_RED, BlizzerProximity.colorArgbFor(200))
        assertEquals(BlizzerProximity.COLOR_RED, BlizzerProximity.colorArgbFor(100))
    }

    test("colorArgbFor returns neutral for null distance") {
        assertEquals(BlizzerProximity.COLOR_NEUTRAL, BlizzerProximity.colorArgbFor(null))
    }

    test("blinkPeriodMillisFor speeds up as distance shrinks") {
        assertEquals(1000, BlizzerProximity.blinkPeriodMillisFor(2000))
        assertEquals(800, BlizzerProximity.blinkPeriodMillisFor(1000))
        assertEquals(650, BlizzerProximity.blinkPeriodMillisFor(500))
        assertEquals(400, BlizzerProximity.blinkPeriodMillisFor(200))
        assertEquals(220, BlizzerProximity.blinkPeriodMillisFor(100))
        assertEquals(900, BlizzerProximity.blinkPeriodMillisFor(null))
    }
}
