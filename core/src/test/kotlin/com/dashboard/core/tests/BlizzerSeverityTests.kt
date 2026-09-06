package com.dashboard.core.tests

import com.dashboard.core.domain.BlizzerSeverity
import com.dashboard.core.domain.BlizzerSeverityMapper
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals

fun blizzerSeverityMapperSuite() = TestSuite("BlizzerSeverityMapper").apply {
    test("null distance is INFO (neutral)") {
        assertEquals(BlizzerSeverity.INFO, BlizzerSeverityMapper.forDistance(null), "null -> INFO")
    }
    test("beyond 1000m is DISTANT (blue)") {
        assertEquals(BlizzerSeverity.DISTANT, BlizzerSeverityMapper.forDistance(2000), "2000m -> DISTANT")
        assertEquals(BlizzerSeverity.DISTANT, BlizzerSeverityMapper.forDistance(1001), "1001m -> DISTANT")
    }
    test("501m to 1000m is APPROACHING (green)") {
        assertEquals(BlizzerSeverity.APPROACHING, BlizzerSeverityMapper.forDistance(1000), "1000m -> APPROACHING")
        assertEquals(BlizzerSeverity.APPROACHING, BlizzerSeverityMapper.forDistance(501), "501m -> APPROACHING")
    }
    test("500m and below is CLOSE (red)") {
        assertEquals(BlizzerSeverity.CLOSE, BlizzerSeverityMapper.forDistance(500), "500m -> CLOSE")
        assertEquals(BlizzerSeverity.CLOSE, BlizzerSeverityMapper.forDistance(200), "200m -> CLOSE")
        assertEquals(BlizzerSeverity.CLOSE, BlizzerSeverityMapper.forDistance(100), "100m -> CLOSE")
        assertEquals(BlizzerSeverity.CLOSE, BlizzerSeverityMapper.forDistance(0), "0m -> CLOSE")
    }
}
