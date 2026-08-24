package com.dashboard.phoneapp

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.dashboard.core.communication.MessageCodec
import com.dashboard.core.communication.NavigationAnnouncementParser
import com.dashboard.core.communication.toProtocol
import com.dashboard.core.domain.NavigationState

/**
 * Captures spoken turn-by-turn announcements from Google Maps or Waze and turns them into
 * [NavigationState] checkpoints via [NavigationAnnouncementParser] — the SAME parser used by
 * `core`'s [com.dashboard.core.hardware.mock.MockPhoneCommunication.announceNavigation] dev
 * control, so the exact logic already tested against realistic phrasing there is what runs here
 * against real, captured text.
 *
 * ## Status: written, NOT yet run against a real device
 * The one thing this class cannot do without a physical phone running actual Google Maps/Waze
 * navigation is confirm that [onAccessibilityEvent] actually receives the announcement text this
 * was written to expect. That assumption (see docs/android-integration-research.md) needs to be
 * verified before this is relied on — the recommended first test is exactly what this class logs:
 * run this service, start turn-by-turn navigation in Google Maps, and check logcat for
 * "Captured raw event text" entries to see what real events actually look like.
 *
 * ## Sending to the watch — not yet wired
 * This currently only logs the parsed checkpoint (see [onCheckpointParsed]). Sending it to the
 * watch requires a [com.dashboard.core.hardware.BluetoothProvider] implementation on this side,
 * which is deliberately not built yet — see the open "raw BLE vs. Wear Data Layer API" question
 * in docs/android-integration-research.md. Resolve that first; this class's `TODO` marks exactly
 * where the real send call goes once it is.
 */
class NavigationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "NavAccessibilityService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Defense in depth beyond the XML config's packageNames filter — belt and suspenders.
        val sourcePackage = event.packageName?.toString()
        if (sourcePackage != "com.google.android.apps.maps" && sourcePackage != "com.waze") return

        val rawText = event.text?.joinToString(separator = " ") { it.toString() }?.trim()
        if (rawText.isNullOrEmpty()) return

        Log.d(TAG, "Captured raw event text from $sourcePackage: \"$rawText\"")

        val checkpoint = NavigationAnnouncementParser.parse(rawText)
        if (checkpoint == null) {
            Log.d(TAG, "Did not parse as a navigation checkpoint, ignoring")
            return
        }

        onCheckpointParsed(checkpoint)
    }

    private fun onCheckpointParsed(checkpoint: NavigationAnnouncementParser.Checkpoint) {
        Log.i(TAG, "Parsed checkpoint: direction=${checkpoint.direction} " +
            "distanceMeters=${checkpoint.distanceMeters} roadName=${checkpoint.roadName}")

        val navigationState = NavigationState(
            active = true,
            direction = checkpoint.direction,
            distanceMeters = checkpoint.distanceMeters,
            roadName = checkpoint.roadName,
            etaMinutes = null, // announcements don't carry ETA; a real integration would track this separately
        )

        // Proves the encode side of the pipeline works end-to-end even before a real transport
        // exists — this is exactly what would go over Bluetooth to the watch.
        val encodedBytes = MessageCodec.encode(navigationState.toProtocol())
        Log.d(TAG, "Encoded as ${encodedBytes.size} bytes, ready to send once a BluetoothProvider is wired")

        // TODO: once a real BluetoothProvider exists on this side (see class doc), replace this
        // log line with: bluetoothProvider.send(encodedBytes)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }
}
