package com.dashboard.core.blizzer

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.BlizzerEventType
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/** A known speed-camera / enforcement point. `id` is stable so the same camera keeps one event. */
data class CameraPoi(
    val id: String,
    val lat: Double,
    val lon: Double,
    val kind: String = "speed_camera",
    val roadName: String? = null,
)

/**
 * Pure geo + threshold logic for the Blizzer camera-proximity alert. The phone companion owns
 * GPS and the POI dataset; this object turns "where am I / where are the cameras" into "fire an
 * alert at this threshold" with no Android and no side effects, so it is fully unit-tested.
 *
 * Thresholds match `BlizzerProximity` on the watch side: 2000/1000/500/200/100 m.
 */
object CameraProximity {

    val DEFAULT_THRESHOLDS: IntArray = intArrayOf(2000, 1000, 500, 200, 100)

    /** Don't fire until this far inside a band — absorbs GPS jitter around a boundary. */
    private const val HYSTERESIS_M = 15.0

    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val METERS_PER_DEG_LAT = 111_320.0

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2).let { it * it } +
            cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(a)))
    }

    /**
     * Nearest camera within [maxMeters] of ([lat], [lon]), or null. A cheap lat/lon bounding-box
     * prefilter keeps this O(candidates-in-box) rather than O(all-cameras) on the hot path.
     */
    fun nearest(
        lat: Double,
        lon: Double,
        cameras: List<CameraPoi>,
        maxMeters: Double,
    ): Pair<CameraPoi, Double>? {
        val dLat = maxMeters / METERS_PER_DEG_LAT
        val dLon = maxMeters / (METERS_PER_DEG_LAT * max(0.01, cos(lat.toRadians())))
        var best: CameraPoi? = null
        var bestDist = Double.MAX_VALUE
        for (c in cameras) {
            if (abs(c.lat - lat) > dLat || abs(c.lon - lon) > dLon) continue
            val d = haversineMeters(lat, lon, c.lat, c.lon)
            if (d <= maxMeters && d < bestDist) {
                best = c; bestDist = d
            }
        }
        return best?.let { it to bestDist }
    }

    /**
     * The tightest threshold band newly entered on the way toward the same camera, or null.
     * [prevMeters] is the distance the caller last acted on for this camera (null on first sight).
     * Returns e.g. 200 when the driver has just come inside the 200 m band and wasn't already.
     */
    fun crossedThreshold(
        prevMeters: Double?,
        nowMeters: Double,
        thresholds: IntArray = DEFAULT_THRESHOLDS,
    ): Int? {
        val sorted = thresholds.sortedDescending()
        // Smallest band we're now solidly inside (jitter margin applied).
        val nowInside = sorted.filter { nowMeters <= it - HYSTERESIS_M }.minOrNull() ?: return null
        // Already inside it (or tighter) last time? Not a new crossing.
        if (prevMeters != null && prevMeters <= nowInside) return null
        return nowInside
    }

    fun toBlizzerEvent(poi: CameraPoi, thresholdMeters: Int, eventId: String, nowMillis: Long): BlizzerEvent =
        BlizzerEvent(
            id = eventId,
            type = if (thresholdMeters <= 200) BlizzerEventType.ALERT else BlizzerEventType.WARNING,
            message = poi.roadName?.let { "Speed camera — $it" } ?: "Speed camera ahead",
            timestampMillis = nowMillis,
            active = true,
            distanceMeters = thresholdMeters,
        )

    fun clearedEvent(eventId: String, nowMillis: Long): BlizzerEvent =
        BlizzerEvent(
            id = eventId,
            type = BlizzerEventType.WARNING,
            message = "",
            timestampMillis = nowMillis,
            active = false,
            distanceMeters = null,
        )

    private fun Double.toRadians() = this * PI / 180.0
}
