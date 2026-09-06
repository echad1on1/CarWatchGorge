package com.dashboard.core.tests

import com.dashboard.core.communication.MessageCodec
import com.dashboard.core.communication.ProtocolMessage
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals

fun messageCodecSuite() = TestSuite("MessageCodec").apply {

    test("NavigationUpdate round-trips with all fields present") {
        val original = ProtocolMessage.NavigationUpdate(
            active = true,
            direction = "TURN_LEFT",
            distanceMeters = 250.5,
            roadName = "Main St",
            etaMinutes = 7,
        )
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "NavigationUpdate should round-trip exactly")
    }

    test("NavigationUpdate round-trips with null distance/road/eta (navigation inactive)") {
        val original = ProtocolMessage.NavigationUpdate(
            active = false,
            direction = "UNKNOWN",
            distanceMeters = null,
            roadName = null,
            etaMinutes = null,
        )
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "NavigationUpdate with nulls should round-trip exactly")
    }

    test("MediaUpdate round-trips with all fields present") {
        val original = ProtocolMessage.MediaUpdate(
            title = "Night Drive",
            artist = "Kepler Freeway",
            album = "Ignition",
            playbackState = "PLAYING",
            positionMillis = 12_345L,
            durationMillis = 214_000L,
        )
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "MediaUpdate should round-trip exactly")
    }

    test("MediaUpdate round-trips with null title/artist/album (nothing playing)") {
        val original = ProtocolMessage.MediaUpdate(
            title = null,
            artist = null,
            album = null,
            playbackState = "UNKNOWN",
            positionMillis = 0L,
            durationMillis = 0L,
        )
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "MediaUpdate with nulls should round-trip exactly")
    }

    test("MediaCommandMessage round-trips") {
        val original = ProtocolMessage.MediaCommandMessage(command = "NEXT")
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "MediaCommandMessage should round-trip exactly")
    }

    test("BlizzerTrigger round-trips") {
        val original = ProtocolMessage.BlizzerTrigger(
            id = "blizzer-1",
            type = "WARNING",
            message = "Speed camera in 500m",
            timestampMillis = 999L,
            active = true,
            distanceMeters = 500,
        )
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "BlizzerTrigger should round-trip exactly")
    }

    test("BlizzerTrigger round-trips with null distanceMeters (non-proximity event)") {
        val original = ProtocolMessage.BlizzerTrigger(
            id = "blizzer-2",
            type = "INFO",
            message = "Welcome back!",
            timestampMillis = 999L,
            active = true,
            distanceMeters = null,
        )
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "BlizzerTrigger with null distanceMeters should round-trip exactly")
    }

    test("ConnectionUpdate round-trips") {
        val original = ProtocolMessage.ConnectionUpdate(state = "CONNECTED")
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "ConnectionUpdate should round-trip exactly")
    }

    test("SettingsUpdate round-trips") {
        val original = ProtocolMessage.SettingsUpdate(payload = "layout=minimal")
        val decoded = MessageCodec.decode(MessageCodec.encode(original))
        assertEquals(original, decoded, "SettingsUpdate should round-trip exactly")
    }
}
