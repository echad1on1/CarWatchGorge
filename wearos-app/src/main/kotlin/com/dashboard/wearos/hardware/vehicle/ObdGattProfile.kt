package com.dashboard.wearos.hardware.vehicle

import java.util.UUID

/**
 * The BLE GATT shape of a cheap ELM327-style OBD-II adapter, isolated here so that supporting a
 * different dongle is a one-file change. Most clones expose a UART-like pair of characteristics
 * (one to write AT/OBD commands to, one that notifies with the ASCII reply); a few combine both
 * into a single read/write/notify characteristic.
 *
 * The init sequence is the standard "make the adapter quiet and predictable" set:
 *  - `ATZ`   reset
 *  - `ATE0`  echo off (don't send our command back)
 *  - `ATL0`  linefeeds off
 *  - `ATS0`  spaces off (compact hex — the parser handles either)
 *  - `ATH0`  headers off
 *  - `ATSP0` automatic protocol detection
 */
object ObdGattProfile {

    data class Candidate(
        val service: UUID,
        val write: UUID,
        val notify: UUID,
    )

    private fun u(short: String): UUID = UUID.fromString("0000$short-0000-1000-8000-00805f9b34fb")

    /** CCCD — enables notifications on a characteristic. */
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = u("2902")

    /** Tried in order against a connected device's discovered services. */
    val CANDIDATES: List<Candidate> = listOf(
        // Most common: FFF0 service, FFF1 notify, FFF2 write.
        Candidate(service = u("fff0"), write = u("fff2"), notify = u("fff1")),
        // Nordic-UART-like single characteristic (write == notify).
        Candidate(service = u("ffe0"), write = u("ffe1"), notify = u("ffe1")),
        // Some Vgate/Veepeak use 18F0 / 2AF0-2AF1.
        Candidate(service = u("18f0"), write = u("2af1"), notify = u("2af0")),
    )

    val INIT_COMMANDS: List<String> = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")

    /** Device-name fragments that identify an OBD adapter during a scan (upper-cased match). */
    val NAME_HINTS: List<String> = listOf("OBD", "ELM", "VLINK", "VEEPEAK", "VGATE", "OBDII", "ICAR")

    /** Build the Mode-01 request string for a PID, e.g. 0x0C -> "010C". */
    fun modeOneRequest(pid: Int): String = "01" + pid.toString(16).uppercase().padStart(2, '0')
}
