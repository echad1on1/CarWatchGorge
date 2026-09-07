package com.dashboard.phoneapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.dashboard.phoneapp.blizzer.CameraProximityService

/**
 * Setup screen for the phone companion. Two one-time grants:
 *  - Accessibility, so [NavigationAccessibilityService] can read the turn-by-turn text Google
 *    Maps / Waze put on screen;
 *  - Notification access, which is what lets [media.MediaNotificationListenerService] read the
 *    active media session of any music/podcast app.
 * Nothing else on the phone is read. The dashboard watch runs standalone and simply accepts
 * whatever this app sends while the two are connected.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val explanation = TextView(this).apply {
            text = "Dashboard Companion feeds your connected dashboard watch:\n\n" +
                "• turn-by-turn directions read from Google Maps or Waze while navigating\n" +
                "• the currently-playing song and its play/pause/skip controls\n" +
                "• speed-camera proximity warnings from your location\n\n" +
                "It reads nothing else and stores nothing. Grant the two permissions below once."
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        val accessibilityButton = Button(this).apply {
            text = "Grant Accessibility access (navigation)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val notificationButton = Button(this).apply {
            text = "Grant Notification access (music)"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            }
        }

        val cameraAlertsButton = Button(this).apply {
            text = "Start speed-camera alerts"
            setOnClickListener { requestLocationThenStartProximity() }
        }

        val stopCameraAlertsButton = Button(this).apply {
            text = "Stop speed-camera alerts"
            setOnClickListener { stopService(Intent(this@MainActivity, CameraProximityService::class.java)) }
        }

        status = TextView(this).apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 32, 0, 0)
        }

        layout.addView(explanation)
        layout.addView(accessibilityButton)
        layout.addView(notificationButton)
        layout.addView(cameraAlertsButton)
        layout.addView(stopCameraAlertsButton)
        layout.addView(status)
        setContentView(layout)
    }

    private fun requestLocationThenStartProximity() {
        val fine = ContextCompat_checkFine()
        if (!fine) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION,
            )
            return
        }
        // Background location is a second, separate grant on API 29+; ask for it but don't block on it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQ_BG_LOCATION)
        }
        startForegroundService(Intent(this, CameraProximityService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            requestLocationThenStartProximity()
        }
    }

    private fun ContextCompat_checkFine(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val REQ_LOCATION = 2001
        const val REQ_BG_LOCATION = 2002
    }

    override fun onResume() {
        super.onResume()
        val a11y = if (isAccessibilityEnabled()) "granted" else "not granted"
        val notif = if (isNotificationAccessEnabled()) "granted" else "not granted"
        status.text = "Accessibility: $a11y\nNotification access: $notif"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = "$packageName/${NavigationAccessibilityService::class.java.name}"
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(flat) }
        while (splitter.hasNext()) if (splitter.next().equals(target, ignoreCase = true)) return true
        return false
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(':').any { it.contains(packageName) }
    }
}
