package com.dashboard.phoneapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val explanation = TextView(this).apply {
            text = "Dashboard Companion reads the on-screen turn-by-turn directions from Google " +
                "Maps or Waze (e.g. \"200 m, Turn left\") so they can appear on your connected " +
                "dashboard watch.\n\nIt only reads the screen while one of those two apps is open " +
                "and navigating, and cannot see any other app on your phone.\n\nTo enable this, " +
                "grant \"Dashboard Companion\" access in the Accessibility settings screen that " +
                "opens below."
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
