package com.hongducdev.toolkit.car.auto

import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Một điểm tọa độ (độ). */
internal data class GeoPoint(val lat: Double, val lng: Double)

/**
 * Chiếu tọa độ địa lý lên một hình chữ nhật pixel (equirectangular, giữ đúng tỷ lệ),
 * căn giữa và vừa khít toàn bộ các điểm đầu vào.
 */
internal class CarRouteProjection(
    points: List<GeoPoint>,
    private val left: Float,
    private val top: Float,
    private val width: Float,
    private val height: Float
) {
    private val safePoints = points.ifEmpty { listOf(GeoPoint(0.0, 0.0)) }
    private val minLat = safePoints.minOf { it.lat }
    private val maxLat = safePoints.maxOf { it.lat }
    private val minLng = safePoints.minOf { it.lng }
    private val maxLng = safePoints.maxOf { it.lng }
    private val lngScale = cos(Math.toRadians((minLat + maxLat) / 2.0)).coerceAtLeast(0.01)
    private val spanX = max((maxLng - minLng) * lngScale, 1e-6)
    private val spanY = max(maxLat - minLat, 1e-6)
    private val scale = min(width / spanX, height / spanY)
    private val offsetX = left + (width - spanX * scale).toFloat() / 2f
    private val offsetY = top + (height - spanY * scale).toFloat() / 2f

    fun x(p: GeoPoint): Float = offsetX + ((p.lng - minLng) * lngScale * scale).toFloat()

    fun y(p: GeoPoint): Float = offsetY + ((maxLat - p.lat) * scale).toFloat()

    companion object {
        /** Đọc danh sách điểm từ geometry GeoJSON LineString của OSRM. */
        fun parseLineString(geoJson: String): List<GeoPoint> = try {
            val coords = JSONObject(geoJson).optJSONArray("coordinates")
            if (coords == null) {
                emptyList()
            } else {
                (0 until coords.length()).mapNotNull { i ->
                    val c = coords.optJSONArray(i)
                    if (c != null && c.length() >= 2) GeoPoint(c.getDouble(1), c.getDouble(0)) else null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
