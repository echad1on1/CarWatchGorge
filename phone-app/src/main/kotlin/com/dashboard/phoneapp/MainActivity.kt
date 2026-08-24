package com.dashboard.phoneapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal disclosure + settings-launch screen. Built with plain Views rather than Compose
 * deliberately — this app is a single simple screen, and keeping it dependency-light (no Compose
 * setup) matches its actual scope.
 *
 * This screen exists specifically to satisfy the in-app disclosure requirement that comes with
 * using [android.accessibilityservice.AccessibilityService] for a non-accessibility purpose (see
 * NavigationAccessibilityService's class doc and docs/android-integration-research.md) — a user
 * should never have to guess why this app is asking for that permission.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val explanation = TextView(this).apply {
            text = "Dashboard Companion reads spoken turn-by-turn directions from Google Maps " +
                "or Waze (e.g. \"In 200 meters, turn left\") so they can appear on your connected " +
                "dashboard watch.\n\nIt only listens while one of those two apps is actively " +
                "navigating, and cannot see any other app on your phone.\n\nTo enable this, grant " +
                "\"Dashboard Companion\" access in the Accessibility settings screen that opens " +
                "below."
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        val openSettingsButton = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        layout.addView(explanation)
        layout.addView(openSettingsButton)
        setContentView(layout)
    }
}
