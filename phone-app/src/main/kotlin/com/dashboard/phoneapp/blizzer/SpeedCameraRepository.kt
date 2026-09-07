package com.dashboard.phoneapp.blizzer

import android.content.Context
import android.util.Log
import com.dashboard.core.blizzer.CameraPoi
import org.json.JSONObject

/**
 * Loads the speed-camera POI dataset. v1 reads a bundled GeoJSON asset
 * (`assets/speed_cameras.geojson`) sourced from OpenStreetMap (`highway=speed_camera` /
 * `enforcement=*`, ODbL — attribution belongs in the app's about screen). A later version can
 * download + cache a fuller regional extract in app storage behind this same class; nothing
 * above it needs to change.
 *
 * Expected GeoJSON: a `FeatureCollection` of `Point` features. `properties.id` /
 * `properties.name` / `properties.ref` are used when present.
 */
class SpeedCameraRepository(private val context: Context) {

    private companion object {
        const val TAG = "SpeedCameraRepo"
        const val ASSET = "speed_cameras.geojson"
    }

    @Volatile
    private var cache: List<CameraPoi>? = null

    fun all(): List<CameraPoi> {
        cache?.let { return it }
        val loaded = runCatching { parse(context.assets.open(ASSET).bufferedReader().use { it.readText() }) }
            .getOrElse {
                Log.w(TAG, "could not load $ASSET: ${it.message}")
                emptyList()
            }
        cache = loaded
        Log.i(TAG, "loaded ${loaded.size} speed cameras")
        return loaded
    }

    private fun parse(json: String): List<CameraPoi> {
        val root = JSONObject(json)
        val features = root.optJSONArray("features") ?: return emptyList()
        val out = ArrayList<CameraPoi>(features.length())
        for (i in 0 until features.length()) {
            val f = features.optJSONObject(i) ?: continue
            val geom = f.optJSONObject("geometry") ?: continue
            if (geom.optString("type") != "Point") continue
            val coords = geom.optJSONArray("coordinates") ?: continue
            if (coords.length() < 2) continue
            val lon = coords.optDouble(0)
            val lat = coords.optDouble(1)
            if (lat.isNaN() || lon.isNaN()) continue
            val props = f.optJSONObject("properties") ?: JSONObject()
            val id = props.optString("id").ifEmpty {
                props.optString("@id").ifEmpty { "cam_${lat}_${lon}" }
            }
            val road = props.optString("name").ifEmpty { props.optString("ref").ifEmpty { null.toString() } }
                .takeIf { it.isNotEmpty() && it != "null" }
            out += CameraPoi(id = id, lat = lat, lon = lon, roadName = road)
        }
        return out
    }
}
