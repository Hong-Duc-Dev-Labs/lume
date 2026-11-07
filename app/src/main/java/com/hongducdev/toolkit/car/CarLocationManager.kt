package com.hongducdev.toolkit.car

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class CarLocationState(
    val latitude: Double = 21.0285, // Mặc định Hà Nội
    val longitude: Double = 105.8542,
    val speedKmh: Int = 0,
    val bearing: Float = 0f,
    val altitude: Double = 0.0,
    val hasGpsFix: Boolean = false,
    val isPermissionGranted: Boolean = false
)

class CarLocationManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _locationState = MutableStateFlow(CarLocationState())
    val locationState: StateFlow<CarLocationState> = _locationState.asStateFlow()

    private var isTracking = false

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speedKmh = if (location.hasSpeed() && location.speed > 0f) {
                (location.speed * 3.6f).roundToInt()
            } else {
                0
            }
            _locationState.value = _locationState.value.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                speedKmh = speedKmh,
                bearing = if (location.hasBearing()) location.bearing else _locationState.value.bearing,
                altitude = location.altitude,
                hasGpsFix = true
            )
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

        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        _locationState.value = _locationState.value.copy(isPermissionGranted = hasPermission)
        if (!hasPermission) return

        try {
            isTracking = true
            // Đọc vị trí lưu gần nhất nếu có
            val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLast = lastGps ?: lastNet
            if (bestLast != null) {
                _locationState.value = _locationState.value.copy(
                    latitude = bestLast.latitude,
                    longitude = bestLast.longitude,
                    bearing = bestLast.bearing,
                    hasGpsFix = true
                )
            }

            // Đăng ký cập nhật định vị liên tục (khoảng 1 giây một lần hoặc mỗi 1 mét di chuyển)
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    gpsListener,
                    Looper.getMainLooper()
                )
            }
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
