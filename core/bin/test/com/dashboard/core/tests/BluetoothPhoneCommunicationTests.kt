package com.dashboard.core.tests

import com.dashboard.core.communication.BluetoothPhoneCommunication
import com.dashboard.core.communication.MessageCodec
import com.dashboard.core.communication.ProtocolMessage
import com.dashboard.core.domain.MediaCommand
import com.dashboard.core.hardware.mock.LoopbackBluetoothProvider
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertTrue

/**
 * Unlike [mockPhoneCommunicationSuite], these tests exercise [BluetoothPhoneCommunication]
 * across two genuinely separate [LoopbackBluetoothProvider] endpoints — bytes are actually
 * produced by [MessageCodec.encode], actually cross the "wire", and are actually decoded on the
 * other side. This is the concrete proof that the protocol doesn't depend on real Bluetooth
 * hardware: swap the loopback pair for a real BLE link and this class needs no changes.
 */
fun bluetoothPhoneCommunicationSuite() = TestSuite("BluetoothPhoneCommunication (loopback transport)").apply {

    test("a NavigationUpdate sent from the phone side arrives decoded on the dashboard side") {
        val (dashboardLink, phoneLink) = LoopbackBluetoothProvider.pair()
        dashboardLink.connect()
        val dashboardComms = BluetoothPhoneCommunication(dashboardLink)

        var received: com.dashboard.core.domain.NavigationState? = null
        dashboardComms.observeNavigationState { received = it }

        val message = ProtocolMessage.NavigationUpdate(
            active = true, direction = "TURN_LEFT", distanceMeters = 300.0, roadName = "5th Ave", etaMinutes = 4,
        )
        phoneLink.send(MessageCodec.encode(message))

        assertTrue(received != null, "dashboard side should have received a decoded NavigationState")
        assertEquals(true, received!!.active, "active should decode correctly across the wire")
        assertEquals("5th Ave", received!!.roadName, "roadName should decode correctly across the wire")
    }

    test("a MediaUpdate sent from the phone side arrives decoded on the dashboard side") {
        val (dashboardLink, phoneLink) = LoopbackBluetoothProvider.pair()
        dashboardLink.connect()
        val dashboardComms = BluetoothPhoneCommunication(dashboardLink)

        var received: com.dashboard.core.domain.MediaState? = null
        dashboardComms.observeMediaState { received = it }

        val message = ProtocolMessage.MediaUpdate(
            title = "Overpass", artist = "Kepler Freeway", album = "Ignition",
            playbackState = "PLAYING", positionMillis = 1000L, durationMillis = 198_000L,
        )
        phoneLink.send(MessageCodec.encode(message))

        assertTrue(received != null, "dashboard side should have received a decoded MediaState")
        assertEquals("Overpass", received!!.title, "title should decode correctly across the wire")
    }

    test("a media command sent from the dashboard side arrives decoded on the phone side") {
        val (dashboardLink, phoneLink) = LoopbackBluetoothProvider.pair()
        dashboardLink.connect()
        val dashboardComms = BluetoothPhoneCommunication(dashboardLink)

        var receivedBytes: ByteArray? = null
        phoneLink.observeRawInbound { receivedBytes = it }

        dashboardComms.sendMediaCommand(MediaCommand.NEXT)

        assertTrue(receivedBytes != null, "phone side should have received bytes")
        val decoded = MessageCodec.decode(receivedBytes!!) as ProtocolMessage.MediaCommandMessage
        assertEquals("NEXT", decoded.command, "command should decode correctly across the wire")
    }
}
