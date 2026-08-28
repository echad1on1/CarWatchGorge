package com.dashboard.phoneapp

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dashboard.core.communication.MessageCodec
import com.dashboard.core.communication.NavigationAnnouncementParser
import com.dashboard.core.communication.toProtocol
import com.dashboard.core.domain.NavigationState

class NavigationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "NavAccessibilityService"
        private const val MIN_RESCAN_INTERVAL_MILLIS = 400L
    }

    private var lastScanTimestampMillis = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val sourcePackage = event.packageName?.toString()
        if (sourcePackage != "com.google.android.apps.maps" && sourcePackage != "com.waze") return

        val now = System.currentTimeMillis()
        if (now - lastScanTimestampMillis < MIN_RESCAN_INTERVAL_MILLIS) return
        lastScanTimestampMillis = now

        val root = rootInActiveWindow ?: return
        val textFragments = mutableListOf<String>()
        collectVisibleText(root, textFragments)
        root.recycle()

        if (textFragments.isEmpty()) return
        val screenTextBlob = textFragments.joinToString(" | ")
        Log.d(TAG, "Screen text from $sourcePackage: \"$screenTextBlob\"")

        val checkpoint = NavigationAnnouncementParser.parse(screenTextBlob)
        if (checkpoint == null) {
            Log.d(TAG, "Did not parse as a navigation checkpoint, ignoring")
            return
        }

        onCheckpointParsed(checkpoint)
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int = 0) {
        if (node == null || depth > 50) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectVisibleText(child, out, depth + 1)
            child.recycle()
        }
    }

    private fun onCheckpointParsed(checkpoint: NavigationAnnouncementParser.Checkpoint) {
        Log.i(TAG, "Parsed checkpoint: direction=${checkpoint.direction} " +
            "distanceMeters=${checkpoint.distanceMeters} roadName=${checkpoint.roadName}")

        val navigationState = NavigationState(
            active = true,
            direction = checkpoint.direction,
            distanceMeters = checkpoint.distanceMeters,
            roadName = checkpoint.roadName,
            etaMinutes = null,
        )

        val encodedBytes = MessageCodec.encode(navigationState.toProtocol())
        Log.d(TAG, "Encoded as ${encodedBytes.size} bytes, ready to send once a BluetoothProvider is wired")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }
}
