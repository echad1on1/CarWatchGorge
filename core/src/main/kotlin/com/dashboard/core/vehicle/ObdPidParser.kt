package com.dashboard.core.vehicle

import com.dashboard.core.domain.Signal
import com.dashboard.core.domain.VehicleData

/**
 * Pure, Android-free parsing of ELM327-style OBD-II responses (Service/Mode 01 — current data).
 * The watch-side `BleObdVehicleDataProvider` owns the BLE GATT link and the AT-command setup;
 * everything from "raw ASCII the adapter sent back" onward lives here so the byte maths is
 * unit-tested without a dongle (getting a conversion wrong is the most likely silent bug).
 *
 * Not covered on purpose:
 *  - Gear position — there is no standard OBD-II PID for it, so it stays `Signal.Unavailable`.
 *  - Oil pressure — likewise no standard Mode-01 PID; stays `Signal.Unavailable`.
 */
object ObdPidParser {

    /** One decoded `41 PP D0 D1 …` response: mode is always 0x41 here, [data] are the payload bytes. */
    data class ObdReading(val pid: Int, val data: List<Int>)

    /** Mode-01 PIDs this dashboard polls, with their payload length in bytes. */
    private val PID_LENGTHS: Map<Int, Int> = mapOf(
        0x04 to 1, // calculated engine load
        0x05 to 1, // engine coolant temperature
        0x0C to 2, // engine RPM
        0x0D to 1, // vehicle speed
        0x11 to 1, // throttle position
        0x2F to 1, // fuel tank level input
        0x42 to 2, // control module (battery) voltage
        0x5C to 1, // engine oil temperature
    )

    val POLLED_PIDS: List<Int> get() = PID_LENGTHS.keys.sorted()

    private val ERROR_MARKERS = listOf(
        "NO DATA", "UNABLE TO CONNECT", "STOPPED", "SEARCHING", "BUS INIT", "BUSINIT",
        "CAN ERROR", "BUS ERROR", "FB ERROR", "DATA ERROR", "ERROR", "?", "ACT ALERT",
        "LP ALERT", "BUFFER FULL",
    )

    /**
     * Parse whatever the adapter returned for one or more PID requests. Tolerates ELM327 noise:
     * echoed command, `\r`/`\n` line breaks, the `>` prompt, spaces or no spaces between bytes,
     * headers, and status lines like `SEARCHING...` / `NO DATA` (those yield no readings).
     * A multi-PID response (`41 0C 1A F8 0D 40`) yields one [ObdReading] per PID.
     */
    fun parseResponse(raw: String): List<ObdReading> {
        val upper = raw.uppercase()
        if (ERROR_MARKERS.any { it != "?" && upper.contains(it) } || upper.trim() == "?") return emptyList()

        val bytes = tokenizeHexBytes(upper)
        val readings = mutableListOf<ObdReading>()
        var i = 0
        while (i < bytes.size) {
            // A positive Mode-01 reply starts with 0x41. Skip anything up to it (headers, echoes).
            if (bytes[i] != 0x41) { i++; continue }
            i++ // consume 0x41
            // Then a run of (pid, data…) groups until the next 0x41 or end.
            while (i < bytes.size && bytes[i] != 0x41) {
                val pid = bytes[i]
                val len = PID_LENGTHS[pid]
                if (len == null || i + len >= bytes.size) { i++; continue }
                readings += ObdReading(pid, bytes.subList(i + 1, i + 1 + len).toList())
                i += 1 + len
            }
        }
        return readings
    }

    private fun tokenizeHexBytes(text: String): List<Int> {
        // Keep only hex digits and whitespace, then split into tokens. Handles "410C1AF8",
        // "41 0C 1A F8", and (headers on) "7E8 06 41 0C 1A F8" — the odd-length CAN-id token
        // "7E8" is dropped, the rest are read as byte pairs.
        val cleaned = text.replace(Regex("[^0-9A-F\\s]"), " ")
        val out = mutableListOf<Int>()
        for (token in cleaned.trim().split(Regex("\\s+"))) {
            if (token.length < 2 || token.length % 2 != 0) continue
            var k = 0
            while (k + 2 <= token.length) {
                token.substring(k, k + 2).toIntOrNull(16)?.let { out += it }
                k += 2
            }
        }
        return out
    }

    // --- per-PID decoders (all pure) ---------------------------------------------------------

    private fun a(r: ObdReading) = r.data.getOrNull(0)
    private fun b(r: ObdReading) = r.data.getOrNull(1)

    fun engineRpm(r: ObdReading): Int? {
        val a = a(r) ?: return null; val b = b(r) ?: return null
        return (a * 256 + b) / 4
    }

    fun speedKmh(r: ObdReading): Int? = a(r)

    fun coolantTempC(r: ObdReading): Int? = a(r)?.minus(40)

    fun oilTempC(r: ObdReading): Int? = a(r)?.minus(40)

    fun engineLoadPercent(r: ObdReading): Double? = a(r)?.let { it * 100.0 / 255.0 }

    fun throttlePercent(r: ObdReading): Double? = a(r)?.let { it * 100.0 / 255.0 }

    fun fuelLevelPercent(r: ObdReading): Double? = a(r)?.let { it * 100.0 / 255.0 }

    fun controlModuleVoltage(r: ObdReading): Double? {
        val a = a(r) ?: return null; val b = b(r) ?: return null
        return (a * 256 + b) / 1000.0
    }

    /**
     * Fold a set of readings into a [VehicleData]. PIDs not present in [readings] stay
     * [Signal.Unavailable] — never fabricated (the `Signal` doctrine).
     */
    fun toVehicleData(readings: List<ObdReading>, nowMillis: Long): VehicleData {
        val byPid = readings.associateBy { it.pid }
        fun <T> sig(pid: Int, decode: (ObdReading) -> T?): Signal<T> {
            val r = byPid[pid] ?: return Signal.Unavailable
            val v = decode(r) ?: return Signal.Unavailable
            return Signal.Available(v, nowMillis)
        }
        return VehicleData(
            speedKmh = sig(0x0D) { speedKmh(it)?.toDouble() },
            rpm = sig(0x0C) { engineRpm(it) },
            gear = Signal.Unavailable, // no standard OBD-II PID
            coolantTempCelsius = sig(0x05) { coolantTempC(it)?.toDouble() },
            oilPressureKpa = Signal.Unavailable, // no standard OBD-II PID
            oilTempCelsius = sig(0x5C) { oilTempC(it)?.toDouble() },
            engineLoadPercent = sig(0x04) { engineLoadPercent(it) },
            fuelLevelPercent = sig(0x2F) { fuelLevelPercent(it) },
            batteryVoltage = sig(0x42) { controlModuleVoltage(it) },
            extras = buildMap {
                byPid[0x11]?.let { r -> throttlePercent(r)?.let { put("throttlePercent", Signal.Available(it, nowMillis)) } }
            },
        )
    }
}
