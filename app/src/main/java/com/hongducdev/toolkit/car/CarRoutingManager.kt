package com.hongducdev.toolkit.car

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

private const val TAG = "CarRoutingManager"

data class DestinationItem(
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

data class RouteInfo(
    val destinationName: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val geoJson: String
) {
    val distanceDisplay: String
        get() = if (distanceMeters >= 1000) {
            String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000.0)
        } else {
            "${distanceMeters.toInt()} m"
        }

    val durationDisplay: String
        get() {
            val totalMinutes = (durationSeconds / 60.0).toInt().coerceAtLeast(1)
            return if (totalMinutes >= 60) {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                "${hours} giờ ${mins} phút"
            } else {
                "${totalMinutes} phút"
            }
        }
}

class CarRoutingManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _searchResults = MutableStateFlow<List<DestinationItem>>(emptyList())
    val searchResults: StateFlow<List<DestinationItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _activeRoute = MutableStateFlow<RouteInfo?>(null)
    val activeRoute: StateFlow<RouteInfo?> = _activeRoute.asStateFlow()

    private val _isLoadingRoute = MutableStateFlow(false)
    val isLoadingRoute: StateFlow<Boolean> = _isLoadingRoute.asStateFlow()

    /**
     * Tìm kiếm địa điểm bằng Photon Komoot Geocoder (OpenStreetMap)
     */
    suspend fun searchPlaces(query: String, userLat: Double, userLng: Double): List<DestinationItem> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                _searchResults.value = emptyList()
                return@withContext emptyList()
            }

            _isSearching.value = true
            val results = mutableListOf<DestinationItem>()

            try {
                val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
                val url = "https://photon.komoot.io/api/?q=$encoded&limit=6&lat=$userLat&lon=$userLng"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "ToolkitCarDashboard/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val features = json.optJSONArray("features")
                    if (features != null) {
                        for (i in 0 until features.length()) {
                            val feat = features.getJSONObject(i)
                            val props = feat.optJSONObject("properties") ?: JSONObject()
                            val geom = feat.optJSONObject("geometry") ?: JSONObject()
                            val coords = geom.optJSONArray("coordinates")

                            if (coords != null && coords.length() >= 2) {
                                val lon = coords.getDouble(0)
                                val lat = coords.getDouble(1)

                                val name = props.optString("name").ifEmpty {
                                    props.optString("street", trimmed)
                                }
                                val city = props.optString("city")
                                val state = props.optString("state")
                                val country = props.optString("country", "Việt Nam")

                                val subtitleParts = listOf(city, state, country).filter { it.isNotEmpty() }
                                val subtitle = subtitleParts.joinToString(", ")

                                results.add(
                                    DestinationItem(
                                        name = name,
                                        subtitle = subtitle,
                                        latitude = lat,
                                        longitude = lon
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search places failed", e)
            } finally {
                _isSearching.value = false
                _searchResults.value = results
            }

            results
        }

    /**
     * Tính toán lộ trình lái xe bằng OSRM (Open Source Routing Machine)
     */
    suspend fun calculateRoute(
        startLat: Double,
        startLng: Double,
        destination: DestinationItem
    ): RouteInfo? = withContext(Dispatchers.IO) {
        _isLoadingRoute.value = true
        var routeInfo: RouteInfo? = null

        try {
            val url = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;${destination.longitude},${destination.latitude}?overview=full&geometries=geojson"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ToolkitCarDashboard/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val code = json.optString("code")
                if (code == "Ok") {
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val routeObj = routes.getJSONObject(0)
                        val dist = routeObj.optDouble("distance", 0.0)
                        val dur = routeObj.optDouble("duration", 0.0)
                        val geom = routeObj.optJSONObject("geometry")

                        if (geom != null) {
                            routeInfo = RouteInfo(
                                destinationName = destination.name,
                                destinationLat = destination.latitude,
                                destinationLng = destination.longitude,
                                distanceMeters = dist,
                                durationSeconds = dur,
                                geoJson = geom.toString()
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Calculate route failed", e)
        } finally {
            _isLoadingRoute.value = false
            _activeRoute.value = routeInfo
        }

        routeInfo
    }

    /**
     * Xóa lộ trình hiện tại
     */
    fun clearRoute() {
        _activeRoute.value = null
        _searchResults.value = emptyList()
    }

    /**
     * Bắt đầu điều hướng Turn-by-Turn bằng Google Maps Navigation
     */
    fun startGoogleNavigation(lat: Double, lng: Double, name: String = "") {
        try {
            val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback mở web browser Google Maps
            try {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (ignored: Exception) {
            }
        }
    }

    companion object {
        /**
         * Danh sách các địa điểm tìm nhanh phổ biến cho tài xế
         */
        val QUICK_SEARCH_CHIPS = listOf(
            "Cây xăng gần nhất",
            "Bãi đỗ xe",
            "Sân bay Nội Bài",
            "Sân bay Tân Sơn Nhất",
            "Bệnh viện"
        )
    }
}
