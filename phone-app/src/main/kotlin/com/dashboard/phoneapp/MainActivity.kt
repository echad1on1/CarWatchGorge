package com.dashboard.phoneapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

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

        status = TextView(this).apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 32, 0, 0)
        }

        layout.addView(explanation)
        layout.addView(accessibilityButton)
        layout.addView(notificationButton)
        layout.addView(status)
        setContentView(layout)
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
