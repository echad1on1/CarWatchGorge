package com.dashboard.phoneapp.blizzer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.dashboard.core.blizzer.CameraPoi
import com.dashboard.core.blizzer.CameraProximity
import com.dashboard.core.communication.MessageCodec
import com.dashboard.core.communication.toProtocol
import com.dashboard.phoneapp.WearMessageSender
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Foreground service that watches the phone's location and, as the driver approaches a known
 * speed camera, sends the watch a `BlizzerEvent` at each threshold band (2000/1000/500/200/100 m).
 * All the geo + threshold logic is `core`'s [CameraProximity]; this class is GPS plumbing plus
 * the foreground-service notification the OS requires for background location.
 *
 * Started/stopped from `MainActivity`. No dependency on any other app.
 */
class CameraProximityService : Service() {

    private companion object {
        const val TAG = "CameraProximity"
        const val CHANNEL_ID = "blizzer_proximity"
        const val NOTIF_ID = 42
        const val MAX_RANGE_M = 2_500.0
        const val UPDATE_INTERVAL_MS = 1_000L
    }

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val repo by lazy { SpeedCameraRepository(this) }
    private var cameras: List<CameraPoi> = emptyList()

    private var activeCameraId: String? = null
    private var lastActedDistance: Double? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onFix(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        cameras = repo.all()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Watching for speed cameras"))
        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { fused.removeLocationUpdates(locationCallback) }
        activeCameraId?.let { sendCleared(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS)
            .build()
        try {
            fused.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            Log.w(TAG, "location permission missing: ${e.message}")
            stopSelf()
        }
    }

    private fun onFix(location: Location) {
        val hit = CameraProximity.nearest(location.latitude, location.longitude, cameras, MAX_RANGE_M)
        if (hit == null) {
            activeCameraId?.let { sendCleared(it); activeCameraId = null; lastActedDistance = null }
            return
        }
        val (poi, distance) = hit
        if (poi.id != activeCameraId) {
            // Switched to a different (closer) camera — clear the old alert first.
            activeCameraId?.let { sendCleared(it) }
            activeCameraId = poi.id
            lastActedDistance = null
        }
        val threshold = CameraProximity.crossedThreshold(lastActedDistance, distance)
        if (threshold != null) {
            lastActedDistance = distance
            val event = CameraProximity.toBlizzerEvent(poi, threshold, poi.id, System.currentTimeMillis())
            WearMessageSender.send(applicationContext, MessageCodec.encode(event.toProtocol()))
            Log.i(TAG, "camera ${poi.id} at ${distance.toInt()} m -> threshold $threshold")
        }
    }

    private fun sendCleared(cameraId: String) {
        val event = CameraProximity.clearedEvent(cameraId, System.currentTimeMillis())
        WearMessageSender.send(applicationContext, MessageCodec.encode(event.toProtocol()))
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Speed-camera alerts", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        return builder
            .setContentTitle("Dashboard Companion")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }
}
