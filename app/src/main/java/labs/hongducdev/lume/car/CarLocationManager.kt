package labs.hongducdev.lume.car

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class CarLocationState(
    val latitude: Double = 21.0285, // Mặc định Hà Nội (nếu chưa từng có fix nào)
    val longitude: Double = 105.8542,
    val speedKmh: Int = 0,
    val bearing: Float = 0f,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val hasGpsFix: Boolean = false,
    val isPermissionGranted: Boolean = false
)

class CarLocationManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val prefs = context.getSharedPreferences("car_location_prefs", Context.MODE_PRIVATE)

    private val _locationState = MutableStateFlow(loadInitialLocationState())
    val locationState: StateFlow<CarLocationState> = _locationState.asStateFlow()

    private var isTracking = false
    private var currentBestLocation: Location? = null

    init {
        // Cố gắng lấy ngay vị trí tốt nhất có sẵn từ hệ thống ngay khi khởi tạo
        checkAndApplyLastKnownLocation()
    }

    private fun loadInitialLocationState(): CarLocationState {
        val savedLat = prefs.getString("last_lat", null)?.toDoubleOrNull()
        val savedLng = prefs.getString("last_lng", null)?.toDoubleOrNull()
        val savedBearing = prefs.getFloat("last_bearing", 0f)

        return if (savedLat != null && savedLng != null) {
            CarLocationState(
                latitude = savedLat,
                longitude = savedLng,
                bearing = savedBearing,
                hasGpsFix = false
            )
        } else {
            CarLocationState()
        }
    }

    private fun saveLocation(location: Location) {
        prefs.edit()
            .putString("last_lat", location.latitude.toString())
            .putString("last_lng", location.longitude.toString())
            .putFloat("last_bearing", location.bearing)
            .apply()
    }

    /**
     * Thuật toán lọc vị trí: Ngăn chặn cell tower / wifi location độ chính xác kém
     * ghi đè hoặc nhảy lệch vị trí GPS độ chính xác cao.
     */
    private fun isBetterLocation(location: Location, currentBest: Location?): Boolean {
        if (currentBest == null) return true

        val timeDelta = location.time - currentBest.time
        val isSignificantlyNewer = timeDelta > 30_000L
        val isSignificantlyOlder = timeDelta < -30_000L
        val isNewer = timeDelta > 0

        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) return false

        val accuracyDelta = (location.accuracy - currentBest.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 150

        val isFromSameProvider = location.provider == currentBest.provider

        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isSignificantlyLessAccurate && isFromSameProvider -> true
            else -> false
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(lm: LocationManager): Location? {
        val providers = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providers.add(LocationManager.FUSED_PROVIDER)
        }
        providers.add(LocationManager.GPS_PROVIDER)
        providers.add(LocationManager.NETWORK_PROVIDER)
        providers.add(LocationManager.PASSIVE_PROVIDER)

        var best: Location? = null
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (best == null) {
                    best = loc
                } else {
                    val isSignificantlyNewer = loc.time - best.time > 60_000L
                    val isNewer = loc.time > best.time
                    val isMoreAccurate = loc.hasAccuracy() && (!best.hasAccuracy() || loc.accuracy < best.accuracy)
                    if (isSignificantlyNewer || (isNewer && isMoreAccurate)) {
                        best = loc
                    }
                }
            } catch (_: Exception) {}
        }
        return best
    }

    private fun checkAndApplyLastKnownLocation() {
        val lm = locationManager ?: return
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return

        try {
            val best = getBestLastKnownLocation(lm)
            if (best != null) {
                applyLocation(best, isDirectFix = true)
            }
        } catch (e: Exception) {
            Log.e("CarLocationManager", "Error reading last known location", e)
        }
    }

    private fun applyLocation(location: Location, isDirectFix: Boolean = true) {
        if (!isBetterLocation(location, currentBestLocation) && !isDirectFix) {
            return
        }
        currentBestLocation = location
        saveLocation(location)

        val speedKmh = if (location.hasSpeed() && location.speed > 0f) {
            (location.speed * 3.6f).roundToInt()
        } else {
            0
        }

        // Deadband góc xoay: Khi đứng yên hoặc < 3 km/h giữ nguyên bearing cũ
        val newBearing = if (location.hasBearing() && speedKmh >= 3) {
            location.bearing
        } else {
            _locationState.value.bearing
        }

        _locationState.value = _locationState.value.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            speedKmh = speedKmh,
            bearing = newBearing,
            altitude = location.altitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 0f,
            hasGpsFix = true,
            isPermissionGranted = true
        )
    }

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            applyLocation(location, isDirectFix = false)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                _locationState.value = _locationState.value.copy(hasGpsFix = false, speedKmh = 0)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        val lm = locationManager ?: return

        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasPermission = hasFine || hasCoarse
        _locationState.value = _locationState.value.copy(isPermissionGranted = hasPermission)
        if (!hasPermission) return

        try {
            isTracking = true

            // Đọc ngay vị trí tốt nhất hiện có
            checkAndApplyLastKnownLocation()

            // Yêu cầu vị trí tươi mới ngay lập tức (API 30+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val provider = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
                    hasFine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    else -> LocationManager.NETWORK_PROVIDER
                }
                try {
                    lm.getCurrentLocation(
                        provider,
                        null,
                        ContextCompat.getMainExecutor(context)
                    ) { loc ->
                        if (loc != null) {
                            applyLocation(loc, isDirectFix = true)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CarLocationManager", "getCurrentLocation failed: ${e.message}")
                }
            }

            // Đăng ký listener cập nhật liên tục:
            // 1. FUSED provider (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER,
                    1000L,
                    1f,
                    gpsListener,
                    Looper.getMainLooper()
                )
            }

            // 2. GPS provider (vệ tinh, độ chính xác cao nhất cho xe hơi)
            if (hasFine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    gpsListener,
                    Looper.getMainLooper()
                )
            }

            // 3. Network provider (dự phòng khi mất tín hiệu vệ tinh)
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    5f,
                    gpsListener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            Log.e("CarLocationManager", "Failed to start location updates", e)
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        try {
            locationManager?.removeUpdates(gpsListener)
        } catch (e: Exception) {
            Log.e("CarLocationManager", "Failed to stop location updates", e)
        }
        isTracking = false
    }
}
