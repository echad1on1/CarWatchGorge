package com.dashboard.core.service

import com.dashboard.core.domain.BlizzerEventType
import com.dashboard.core.domain.Direction
import com.dashboard.core.domain.PowerState
import com.dashboard.core.hardware.mock.MockNfcProvider
import com.dashboard.core.hardware.mock.MockPhoneCommunication
import com.dashboard.core.hardware.mock.MockPowerProvider
import com.dashboard.core.hardware.mock.MockVehicleDataProvider

/**
 * Single facade for every developer control described in the spec: Simulate NFC Tap, Connect/
 * Disconnect Phone, Start/Stop Navigation, Change Direction, Start/Pause/Next/Previous Music,
 * Trigger Blizzer, and vehicle-driving simulation. These exist only for development — a real
 * device replaces [MockNfcProvider]/[MockPhoneCommunication]/[MockVehicleDataProvider] entirely,
 * at which point this class (or the screen that binds to it) is removed or hidden.
 *
 * Deliberately depends on the *mock* implementations, not the interfaces — it has no reason to
 * exist once real hardware is in place, so it's fine for it to know it's talking to mocks.
 */
class DevControlPanel(
    private val connectionManager: ConnectionManager,
    private val nfcProvider: MockNfcProvider,
    private val vehicleProvider: MockVehicleDataProvider,
    private val phoneCommunication: MockPhoneCommunication,
    private val powerProvider: MockPowerProvider,
) {
    // Connection
    fun simulateNfcTap() = nfcProvider.simulateTap()
    fun disconnectPhone() = connectionManager.simulateDisconnect()

    // Vehicle
    fun setTargetSpeedKmh(kmh: Double) = vehicleProvider.setTargetSpeedKmh(kmh)

    // Navigation
    fun startNavigation(roadName: String = "Ridge Valley Rd", etaMinutes: Int = 12) =
        phoneCommunication.startNavigation(roadName, etaMinutes)
    fun stopNavigation() = phoneCommunication.stopNavigation()
    fun changeDirection(direction: Direction, distanceMeters: Double) =
        phoneCommunication.changeDirection(direction, distanceMeters)
    fun decreaseDistance(byMeters: Double) = phoneCommunication.decreaseDistance(byMeters)
    /** Simulates the real Maps strategy: feeds raw spoken-announcement text through the parser. */
    fun announceNavigation(rawText: String): Boolean = phoneCommunication.announceNavigation(rawText)

    // Music
    fun startMusic() = phoneCommunication.startMusic()
    fun pauseMusic() = phoneCommunication.pauseMusic()
    fun nextSong() = phoneCommunication.nextSong()
    fun previousSong() = phoneCommunication.previousSong()

    // Blizzer
    fun triggerBlizzer(message: String, type: BlizzerEventType = BlizzerEventType.INFO): String =
        phoneCommunication.triggerBlizzer(message, type)
    /** Simulates a camera-proximity beep/alert at the given distance (closer = more urgent). */
    fun triggerCameraWarning(distanceMeters: Int): String = phoneCommunication.triggerCameraWarning(distanceMeters)
    fun dismissBlizzer(id: String) = phoneCommunication.dismissBlizzer(id)
    fun dismissCameraWarning() = phoneCommunication.dismissCameraWarning()

    // Power
    fun simulatePowerState(state: PowerState) = powerProvider.setState(state)
}
