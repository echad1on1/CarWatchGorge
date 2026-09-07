package com.dashboard.core.tests

import com.dashboard.core.domain.Signal
import com.dashboard.core.vehicle.ObdPidParser
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

fun obdPidParserSuite() = TestSuite("ObdPidParser").apply {

    test("decodes a spaced single-PID RPM response") {
        val readings = ObdPidParser.parseResponse("41 0C 1A F8")
        assertEquals(1, readings.size, "one reading")
        assertEquals(0x0C, readings[0].pid, "pid")
        assertEquals(1726, ObdPidParser.engineRpm(readings[0]), "(0x1AF8)/4 = 1726 rpm")
    }

    test("decodes a no-spaces response with a command echo") {
        val readings = ObdPidParser.parseResponse("010D\r410D50\r\r>")
        assertEquals(1, readings.size, "one reading")
        assertEquals(80, ObdPidParser.speedKmh(readings[0]), "0x50 = 80 km/h")
    }

    test("decodes a multi-PID response in one line") {
        val readings = ObdPidParser.parseResponse("41 0C 1A F8 0D 40 05 5A")
        assertEquals(3, readings.size, "rpm + speed + coolant")
        val byPid = readings.associateBy { it.pid }
        assertEquals(1726, ObdPidParser.engineRpm(byPid[0x0C]!!), "rpm")
        assertEquals(64, ObdPidParser.speedKmh(byPid[0x0D]!!), "0x40 = 64 km/h")
        assertEquals(50, ObdPidParser.coolantTempC(byPid[0x05]!!), "0x5A - 40 = 50 C")
    }

    test("tolerates an 11-bit CAN header prefix (ATH1)") {
        val readings = ObdPidParser.parseResponse("7E8 03 41 05 5B")
        assertEquals(1, readings.size, "one reading past the header")
        assertEquals(51, ObdPidParser.coolantTempC(readings[0]), "0x5B - 40 = 51 C")
    }

    test("NO DATA / SEARCHING / ? yield no readings") {
        assertEquals(0, ObdPidParser.parseResponse("NO DATA").size, "NO DATA")
        assertEquals(0, ObdPidParser.parseResponse("SEARCHING...").size, "SEARCHING")
        assertEquals(0, ObdPidParser.parseResponse("?").size, "?")
        assertEquals(0, ObdPidParser.parseResponse("CAN ERROR").size, "CAN ERROR")
    }

    test("a truncated frame (missing the second RPM byte) is dropped, not misread") {
        val readings = ObdPidParser.parseResponse("41 0C 1A")
        assertEquals(0, readings.size, "0x0C needs 2 data bytes")
    }

    test("unit conversions") {
        fun r(pid: Int, vararg d: Int) = ObdPidParser.ObdReading(pid, d.toList())
        assertEquals(0.0, ObdPidParser.engineLoadPercent(r(0x04, 0x00)), "load 0")
        assertTrue(kotlin.math.abs(ObdPidParser.engineLoadPercent(r(0x04, 0xFF))!! - 100.0) < 0.001, "load 255 -> 100%")
        assertEquals(-40, ObdPidParser.coolantTempC(r(0x05, 0x00)), "coolant 0x00 -> -40 C")
        assertTrue(kotlin.math.abs(ObdPidParser.controlModuleVoltage(r(0x42, 0x2F, 0xDC))!! - 12.252) < 0.001, "0x2FDC mV -> 12.252 V")
        assertTrue(kotlin.math.abs(ObdPidParser.fuelLevelPercent(r(0x2F, 0x7F))!! - 49.8039) < 0.01, "half tank")
    }

    test("toVehicleData fills present PIDs and leaves the rest Unavailable") {
        val readings = ObdPidParser.parseResponse("41 0C 0F A0 0D 32")
        val data = ObdPidParser.toVehicleData(readings, nowMillis = 1_000L)
        assertEquals(true, data.rpm.isAvailable, "rpm available")
        assertEquals(true, data.speedKmh.isAvailable, "speed available")
        assertEquals(1000, (data.rpm as Signal.Available).value, "(0x0FA0)/4 = 1000 rpm")
        assertEquals(50.0, (data.speedKmh as Signal.Available).value, "0x32 = 50 km/h")
        assertEquals(Signal.Unavailable, data.coolantTempCelsius, "no coolant PID in this response")
        assertEquals(Signal.Unavailable, data.gear, "gear is never from OBD-II")
        assertEquals(Signal.Unavailable, data.oilPressureKpa, "oil pressure is never from OBD-II")
    }
}
