package com.dashboard.core.tests

import com.dashboard.core.domain.BlizzerEventType
import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.domain.PlaybackState
import com.dashboard.core.domain.Signal
import com.dashboard.core.hardware.mock.MockAudioOutput
import com.dashboard.core.hardware.mock.MockNfcProvider
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.service.BlizzerManager
import com.dashboard.core.service.ConnectionManager
import com.dashboard.core.service.MediaManager
import com.dashboard.core.service.NavigationManager
import com.dashboard.core.service.VehicleDataManager
import com.dashboard.core.testing.TestSuite
import com.dashboard.core.testing.assertEquals
import com.dashboard.core.testing.assertFalse
import com.dashboard.core.testing.assertTrue

/**
 * Wires up the entire app the way [com.dashboard.core.demo.ConsoleDemo] does, then drives and
 * asserts on the exact journey described in the spec:
 *
 * car starts -> vehicle information appears -> NFC tap -> connected -> Maps/Music become
 * available -> navigation starts and updates -> music starts and is controllable -> Blizzer
 * interrupts Car, then Maps, then Music -> phone disconnects -> back to Car mode.
 */
fun endToEndJourneySuite() = TestSuite("End-to-end user journey").apply {

    test("full journey: CAR_ONLY -> NFC -> CONNECTED -> nav/music/blizzer -> disconnect -> CAR_ONLY") {
        // ---- Assemble the app, mirroring ConsoleDemo's composition -----------------------
        val vehicleProvider = FakeVehicleDataProvider() // deterministic: MockVehicleDataProvider ticks on a
        // background Timer by design (it's meant to behave like a real async vehicle interface), so it's
        // not suitable for synchronous assertions here — that's exercised separately in VehicleDataManagerTests.
        val vehicleManager = VehicleDataManager(vehicleProvider)

        val nfcProvider = MockNfcProvider()
        val bluetoothProvider = FakeBluetoothProvider() // deterministic, unlike MockBluetoothProvider's
        // Timer-based delay — same reasoning as the vehicle provider above.
        val connectionManager = ConnectionManager(nfcProvider, bluetoothProvider)

        val phoneCommunication = MockPhoneCommunication()
        val navigationManager = NavigationManager(phoneCommunication)
        val mediaManager = MediaManager(phoneCommunication)
        val blizzerManager = BlizzerManager(phoneCommunication)
        val audioOutput = MockAudioOutput()

        vehicleManager.start()
        vehicleProvider.push(com.dashboard.core.domain.VehicleData(speedKmh = Signal.Available(0.0, 1L)))
        connectionManager.start()
        navigationManager.start()
        mediaManager.start()
        blizzerManager.start()

        // ---- 1. Car starts, vehicle information appears, with no phone at all ------------
        assertEquals(ConnectionState.CAR_ONLY, connectionManager.state, "should start in CAR_ONLY")
        assertTrue(vehicleManager.latest.speedKmh is Signal.Available, "vehicle data should be flowing immediately")
        assertFalse(navigationManager.latest.active, "Maps should not be available before connection")
        assertEquals(null, mediaManager.latest.title, "Music should not be available before connection")

        // ---- 2. Blizzer interrupts the Car panel ------------------------------------------
        val carBlizzerId = phoneCommunication.triggerBlizzer("Welcome back", BlizzerEventType.INFO)
        assertTrue(blizzerManager.currentEvent != null, "Blizzer should overlay the Car panel")
        phoneCommunication.dismissBlizzer(carBlizzerId)
        assertEquals(null, blizzerManager.currentEvent, "Blizzer should clear, returning to Car panel")

        // ---- 3. NFC tap connects the phone -------------------------------------------------
        nfcProvider.simulateTap()
        assertEquals(ConnectionState.CONNECTED, connectionManager.state, "should be CONNECTED after NFC tap")

        // ---- 4. Navigation starts and updates ----------------------------------------------
        phoneCommunication.startNavigation(roadName = "Ridge Valley Rd", etaMinutes = 9)
        assertTrue(navigationManager.latest.active, "Maps should now show active navigation")
        phoneCommunication.changeDirection(com.dashboard.core.domain.Direction.TURN_RIGHT, 150.0)
        assertEquals(com.dashboard.core.domain.Direction.TURN_RIGHT, navigationManager.latest.direction, "direction should update")

        // ---- 5. Music starts and is controllable --------------------------------------------
        phoneCommunication.startMusic()
        assertEquals(PlaybackState.PLAYING, mediaManager.latest.playbackState, "Music should be playing")
        val firstSong = mediaManager.latest.title
        mediaManager.next()
        assertTrue(mediaManager.latest.title != firstSong, "next() should change the track")
        mediaManager.pause()
        assertEquals(PlaybackState.PAUSED, mediaManager.latest.playbackState, "pause() should pause playback")

        // ---- 6. Blizzer interrupts Maps/Music (same manager, no panel-specific code needed) --
        val mapsMusicBlizzerId = phoneCommunication.triggerBlizzer("Check engine soon", BlizzerEventType.WARNING)
        assertTrue(blizzerManager.currentEvent != null, "Blizzer should overlay Maps/Music the same way it did Car")
        assertEquals("Check engine soon", blizzerManager.currentEvent!!.message, "message should match")
        phoneCommunication.dismissBlizzer(mapsMusicBlizzerId)
        assertEquals(null, blizzerManager.currentEvent, "Blizzer should clear again")

        // Navigation/media state should be untouched by the Blizzer interruption.
        assertTrue(navigationManager.latest.active, "navigation should still be active after Blizzer clears")

        // ---- 7. Phone disconnects, dashboard returns to Car mode ------------------------------
        connectionManager.simulateDisconnect()
        assertEquals(ConnectionState.CAR_ONLY, connectionManager.state, "should return to CAR_ONLY on disconnect")

        // Vehicle data must keep flowing regardless of phone connection throughout.
        assertTrue(vehicleManager.latest.speedKmh is Signal.Available, "vehicle data should still be flowing after disconnect")
    }
}
