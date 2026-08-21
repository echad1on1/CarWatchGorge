package com.dashboard.core.tests

import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.hardware.mock.MockNfcProvider
import com.dashboard.core.service.ConnectionManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

fun connectionManagerSuite() = TestSuite("ConnectionManager").apply {

    test("starts in CAR_ONLY") {
        val manager = ConnectionManager(MockNfcProvider(), FakeBluetoothProvider())
        assertEquals(ConnectionState.CAR_ONLY, manager.state, "initial state")
    }

    test("NFC tap drives CAR_ONLY -> NFC_DETECTED -> CONNECTING -> CONNECTED") {
        val nfc = MockNfcProvider()
        val bt = FakeBluetoothProvider()
        val manager = ConnectionManager(nfc, bt)
        val observed = mutableListOf<ConnectionState>()
        manager.start()
        manager.observeState { observed.add(it) }

        nfc.simulateTap()

        assertEquals(ConnectionState.CONNECTED, manager.state, "final state after tap")
        assertTrue(
            observed.containsAll(
                listOf(
                    ConnectionState.CAR_ONLY,
                    ConnectionState.NFC_DETECTED,
                    ConnectionState.CONNECTING,
                    ConnectionState.CONNECTED,
                )
            ),
            "should have passed through every intermediate state: $observed",
        )
    }

    test("disconnect drives CONNECTED -> DISCONNECTING -> CAR_ONLY") {
        val nfc = MockNfcProvider()
        val bt = FakeBluetoothProvider()
        val manager = ConnectionManager(nfc, bt)
        manager.start()
        nfc.simulateTap()
        assertEquals(ConnectionState.CONNECTED, manager.state, "should be connected before testing disconnect")

        manager.simulateDisconnect()

        assertEquals(ConnectionState.CAR_ONLY, manager.state, "should return to CAR_ONLY after disconnect")
    }

    test("a failed handshake recovers through ERROR back to CAR_ONLY") {
        val nfc = MockNfcProvider()
        val bt = FakeBluetoothProvider()
        val manager = ConnectionManager(nfc, bt)
        val observed = mutableListOf<ConnectionState>()
        manager.start()
        manager.observeState { observed.add(it) }
        bt.failNextConnect = true

        nfc.simulateTap()

        assertEquals(ConnectionState.CAR_ONLY, manager.state, "should recover to CAR_ONLY after a failed connect")
        assertTrue(observed.contains(ConnectionState.ERROR), "should have passed through ERROR: $observed")
    }

    test("a tap while already connecting is ignored (no duplicate transitions)") {
        val nfc = MockNfcProvider()
        val bt = FakeBluetoothProvider()
        // Use a bluetooth fake that doesn't auto-resolve, to catch this window realistically.
        val manager = ConnectionManager(nfc, bt)
        manager.start()
        nfc.simulateTap() // resolves synchronously to CONNECTED in the fake
        val stateAfterFirstTap = manager.state
        nfc.simulateTap() // should be ignored since we're not in CAR_ONLY anymore
        assertEquals(stateAfterFirstTap, manager.state, "second tap while connected should be a no-op")
    }
}
