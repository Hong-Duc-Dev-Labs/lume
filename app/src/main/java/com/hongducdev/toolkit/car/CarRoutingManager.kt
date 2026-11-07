package com.hongducdev.toolkit.car

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

private const val TAG = "CarRoutingManager"

data class DestinationItem(
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int = 0
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
            String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
        } else {
            "${distanceMeters.coerceAtLeast(0.0).toInt()} m"
        }

    val durationDisplay: String
        get() {
            if (durationSeconds <= 0.0) return "0 phút"
            val totalMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
            return if (totalMinutes >= 60) {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                if (mins == 0) "${hours} giờ" else "${hours} giờ ${mins} phút"
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

    private val routeRequestId = AtomicLong(0)
    private var activeRouteCall: Call? = null

    private val searchRequestId = AtomicLong(0)
    private var activeSearchCall: Call? = null

    private fun JSONObject.optCleanString(key: String, fallback: String = ""): String {
        if (!has(key) || isNull(key)) return fallback
        val value = optString(key, fallback)
        return if (value.equals("null", ignoreCase = true)) fallback else value.trim()
    }

    private fun computeDistanceMeters(userLat: Double, userLng: Double, lat: Double, lon: Double): Int {
        val results = FloatArray(1)
        return try {
            Location.distanceBetween(userLat, userLng, lat, lon, results)
            results[0].roundToInt()
        } catch (_: Exception) {
            0
        }
    }

    private fun formatDistance(distanceMeters: Int): String {
        return if (distanceMeters < 1000) {
            "$distanceMeters m"
        } else {
            String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
        }
    }

    /**
     * Tìm tiện ích xung quanh xe qua OpenStreetMap Nominatim (lọc chuẩn amenity=fuel, parking, hospital).
     */
    private fun searchNominatimAmenity(
        amenity: String,
        fallbackName: String,
        userLat: Double,
        userLng: Double,
        callHolder: (Call) -> Unit
    ): List<DestinationItem> {
        val delta = 0.18 // ~20km bán kính xung quanh vị trí thực của xe
        val minLon = userLng - delta
        val maxLon = userLng + delta
        val minLat = userLat - delta
        val maxLat = userLat + delta

        val url = "https://nominatim.openstreetmap.org/search?amenity=$amenity&format=json&limit=10&viewbox=$minLon,$maxLat,$maxLon,$minLat&bounded=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ToolkitCarDashboard/1.0")
            .build()

        val call = httpClient.newCall(request)
        callHolder(call)

        call.execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string().orEmpty()
            val array = org.json.JSONArray(body)
            val items = mutableListOf<DestinationItem>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val lat = obj.optDouble("lat", Double.NaN)
                val lon = obj.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue

                val rawName = obj.optCleanString("name")
                val displayName = obj.optCleanString("display_name")
                val name = rawName.ifEmpty { fallbackName }

                // Làm gọn địa chỉ từ display_name: loại bỏ tên đối tượng ở đầu và Việt Nam ở cuối
                val parts = displayName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val addressParts = parts.filter {
                    !it.equals(rawName, ignoreCase = true) &&
                    !it.equals("Việt Nam", ignoreCase = true) &&
                    !it.matches(Regex("^\\d{5}$"))
                }
                val address = addressParts.take(3).joinToString(", ")

                val dist = computeDistanceMeters(userLat, userLng, lat, lon)
                val distStr = formatDistance(dist)
                val subtitle = if (address.isNotEmpty()) "$distStr • $address" else distStr

                items.add(
                    DestinationItem(
                        name = name,
                        subtitle = subtitle,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = dist
                    )
                )
            }
            return items.sortedBy { it.distanceMeters }
        }
    }

    /**
     * Tìm kiếm địa điểm bằng Photon Komoot Geocoder (OpenStreetMap) với lọc kết quả và tính khoảng cách
     */
    private fun searchPhoton(
        query: String,
        userLat: Double,
        userLng: Double,
        filterBusStop: Boolean = true,
        callHolder: (Call) -> Unit
    ): List<DestinationItem> {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val url = "https://photon.komoot.io/api/?q=$encoded&limit=10&lat=$userLat&lon=$userLng"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ToolkitCarDashboard/1.0")
            .build()

        val call = httpClient.newCall(request)
        callHolder(call)

        call.execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val features = json.optJSONArray("features") ?: return emptyList()
            val items = mutableListOf<DestinationItem>()

            for (i in 0 until features.length()) {
                val feat = features.optJSONObject(i) ?: continue
                val props = feat.optJSONObject("properties") ?: JSONObject()
                val geom = feat.optJSONObject("geometry") ?: JSONObject()
                val coords = geom.optJSONArray("coordinates")

                if (coords != null && coords.length() >= 2) {
                    val lon = coords.optDouble(0, Double.NaN)
                    val lat = coords.optDouble(1, Double.NaN)
                    if (lon.isNaN() || lat.isNaN()) continue

                    val osmValue = props.optCleanString("osm_value")
                    if (filterBusStop && osmValue.equals("bus_stop", ignoreCase = true)) {
                        continue // Bỏ qua trạm xe buýt khi tìm cây xăng hoặc địa điểm khác
                    }

                    val name = props.optCleanString("name").ifEmpty {
                        val street = props.optCleanString("street")
                        val house = props.optCleanString("housenumber")
                        if (street.isNotEmpty()) {
                            if (house.isNotEmpty()) "$house $street" else street
                        } else {
                            query
                        }
                    }

                    val district = props.optCleanString("district").ifEmpty { props.optCleanString("county") }
                    val city = props.optCleanString("city")
                    val state = props.optCleanString("state")
                    val subtitleParts = listOf(district, city, state)
                        .filter { it.isNotEmpty() }
                        .distinct()
                    val address = subtitleParts.joinToString(", ")

                    val dist = computeDistanceMeters(userLat, userLng, lat, lon)
                    val distStr = formatDistance(dist)
                    val subtitle = if (address.isNotEmpty()) "$distStr • $address" else distStr

                    items.add(
                        DestinationItem(
                            name = name,
                            subtitle = subtitle,
                            latitude = lat,
                            longitude = lon,
                            distanceMeters = dist
                        )
                    )
                }
            }
            return items.sortedBy { it.distanceMeters }
        }
    }

    /**
     * Tìm kiếm địa điểm thông minh:
     * - Tự động nhận diện cây xăng/nhiên liệu -> ưu tiên tìm amenity=fuel chuẩn quanh xe
     * - Tự động nhận diện bãi đỗ xe -> ưu tiên amenity=parking
     * - Tự động nhận diện bệnh viện -> ưu tiên amenity=hospital
     * - Các địa điểm khác -> tìm qua Photon Komoot kèm khoảng cách và sắp xếp gần nhất trước
     */
    suspend fun searchPlaces(query: String, userLat: Double, userLng: Double): List<DestinationItem> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                _searchResults.value = emptyList()
                return@withContext emptyList()
            }

            val currentId = searchRequestId.incrementAndGet()
            synchronized(this@CarRoutingManager) {
                activeSearchCall?.cancel()
            }

            _isSearching.value = true
            val results = mutableListOf<DestinationItem>()

            try {
                val lower = trimmed.lowercase()
                val isFuel = lower.contains("cây xăng") || lower.contains("xăng") || lower.contains("petrol") || lower.contains("fuel")
                val isParking = lower.contains("bãi đỗ") || lower.contains("bãi xe") || lower.contains("bãi gửi") || lower.contains("parking")
                val isHospital = lower.contains("bệnh viện") || lower.contains("phòng khám") || lower.contains("trạm y tế") || lower.contains("hospital") || lower.contains("clinic")

                when {
                    isFuel -> {
                        // Thử tìm cây xăng chuẩn qua OSM Nominatim trong bán kính xe
                        val fuelItems = try {
                            searchNominatimAmenity("fuel", "Cây xăng", userLat, userLng) { call ->
                                synchronized(this@CarRoutingManager) { activeSearchCall = call }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Nominatim fuel search failed: ${e.message}")
                            emptyList()
                        }

                        if (fuelItems.isNotEmpty()) {
                            results.addAll(fuelItems)
                        } else {
                            // Dự phòng qua Photon: tìm petrolimex & cây xăng
                            val photonFuel = try {
                                searchPhoton("petrolimex", userLat, userLng, filterBusStop = true) { call ->
                                    synchronized(this@CarRoutingManager) { activeSearchCall = call }
                                }
                            } catch (_: Exception) {
                                emptyList()
                            }
                            results.addAll(photonFuel)
                        }
                    }
                    isParking -> {
                        val parkingItems = try {
                            searchNominatimAmenity("parking", "Bãi đỗ xe", userLat, userLng) { call ->
                                synchronized(this@CarRoutingManager) { activeSearchCall = call }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Nominatim parking search failed: ${e.message}")
                            emptyList()
                        }
                        if (parkingItems.isNotEmpty()) {
                            results.addAll(parkingItems)
                        } else {
                            val photonParking = try {
                                searchPhoton("bãi đỗ xe", userLat, userLng, filterBusStop = true) { call ->
                                    synchronized(this@CarRoutingManager) { activeSearchCall = call }
                                }
                            } catch (_: Exception) {
                                emptyList()
                            }
                            results.addAll(photonParking)
                        }
                    }
                    isHospital -> {
                        val hospitalItems = try {
                            searchNominatimAmenity("hospital", "Bệnh viện", userLat, userLng) { call ->
                                synchronized(this@CarRoutingManager) { activeSearchCall = call }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Nominatim hospital search failed: ${e.message}")
                            emptyList()
                        }
                        if (hospitalItems.isNotEmpty()) {
                            results.addAll(hospitalItems)
                        } else {
                            val photonHospital = try {
                                searchPhoton("bệnh viện", userLat, userLng, filterBusStop = true) { call ->
                                    synchronized(this@CarRoutingManager) { activeSearchCall = call }
                                }
                            } catch (_: Exception) {
                                emptyList()
                            }
                            results.addAll(photonHospital)
                        }
                    }
                    else -> {
                        val generalItems = try {
                            searchPhoton(
                                query = trimmed,
                                userLat = userLat,
                                userLng = userLng,
                                filterBusStop = !lower.contains("bus") && !lower.contains("buýt")
                            ) { call ->
                                synchronized(this@CarRoutingManager) { activeSearchCall = call }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "General search failed", e)
                            emptyList()
                        }
                        results.addAll(generalItems)
                    }
                }
            } catch (e: Exception) {
                if (currentId == searchRequestId.get()) {
                    Log.e(TAG, "Search places failed", e)
                }
            } finally {
                if (currentId == searchRequestId.get()) {
                    _isSearching.value = false
                    _searchResults.value = results.sortedBy { it.distanceMeters }
                }
            }

            results.sortedBy { it.distanceMeters }
        }

    /**
     * Tính toán lộ trình lái xe bằng OSRM (Open Source Routing Machine)
     */
    suspend fun calculateRoute(
        startLat: Double,
        startLng: Double,
        destination: DestinationItem
    ): RouteInfo? = withContext(Dispatchers.IO) {
        if (startLat.isNaN() || startLng.isNaN() || destination.latitude.isNaN() || destination.longitude.isNaN()) {
            Log.e(TAG, "Invalid coordinates provided to calculateRoute")
            return@withContext null
        }

        val currentId = routeRequestId.incrementAndGet()
        synchronized(this@CarRoutingManager) {
            activeRouteCall?.cancel()
        }

        _isLoadingRoute.value = true
        var routeInfo: RouteInfo? = null

        try {
            val url = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;${destination.longitude},${destination.latitude}?overview=full&geometries=geojson&steps=false&alternatives=false"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ToolkitCarDashboard/1.0")
                .build()

            val call = httpClient.newCall(request)
            synchronized(this@CarRoutingManager) {
                activeRouteCall = call
            }

            call.execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Calculate route failed: HTTP ${response.code}")
                    return@use
                }

                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val code = json.optString("code")
                if (code == "Ok") {
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val routeObj = routes.optJSONObject(0) ?: return@use
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
                } else {
                    Log.w(TAG, "OSRM returned non-OK code: $code, message=${json.optString("message")}")
                }
            }
        } catch (e: Exception) {
            if (currentId == routeRequestId.get()) {
                Log.e(TAG, "Calculate route failed", e)
            }
        } finally {
            if (currentId == routeRequestId.get()) {
                _isLoadingRoute.value = false
                if (routeInfo != null) {
                    _activeRoute.value = routeInfo
                }
            }
        }

        routeInfo
    }

    /**
     * Xóa lộ trình hiện tại
     */
    fun clearRoute() {
        routeRequestId.incrementAndGet()
        synchronized(this) {
            activeRouteCall?.cancel()
            activeRouteCall = null
        }
        _activeRoute.value = null
        _searchResults.value = emptyList()
    }

    companion object {
        /**
         * Danh sách các địa điểm tìm nhanh phổ biến cho tài xế
         */
        val QUICK_SEARCH_CHIPS = listOf(
            "Cây xăng",
            "Cây xăng gần nhất",
            "Bãi đỗ xe",
            "Sân bay Nội Bài",
            "Sân bay Tân Sơn Nhất",
            "Bệnh viện"
        )
    }
}
