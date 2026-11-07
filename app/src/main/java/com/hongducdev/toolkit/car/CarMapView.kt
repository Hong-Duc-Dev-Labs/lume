package com.hongducdev.toolkit.car

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val TAG = "CarMapView"

// Vector Style URLs từ OpenFreeMap (tương tự kiến trúc G Navigation)
private const val OFM_LIBERTY_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val OFM_DARK_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

// Dự phòng nhanh: Bản đồ OpenStreetMap raster tiêu chuẩn (đầy đủ tên đường tiếng Việt, màu sắc phân biệt)
private val OSM_RASTER_STYLE_JSON = """
{
  "version": 8,
  "name": "OpenStreetMap",
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": [
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "maxzoom": 19
    }
  },
  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": {
        "background-color": "#f2efe9"
      }
    },
    {
      "id": "osm-tiles",
      "type": "raster",
      "source": "osm",
      "minzoom": 0,
      "maxzoom": 19
    }
  ]
}
""".trimIndent()

@Composable
fun CarMapView(
    locationState: CarLocationState,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    followCar: Boolean = true,
    onFollowCarChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }

    val animatedBearing by animateFloatAsState(
        targetValue = locationState.bearing,
        label = "animatedBearing"
    )

    fun applyMapStyle(map: MapLibreMap, dark: Boolean) {
        val styleUrl = if (dark) OFM_DARK_STYLE_URL else OFM_LIBERTY_STYLE_URL
        Log.d(TAG, "Applying style: $styleUrl")
        map.setStyle(Style.Builder().fromUri(styleUrl)) { _ ->
            isStyleLoaded = true
            Log.d(TAG, "Style loaded successfully from $styleUrl")
        }
    }

    val mapView = remember {
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true) // Bắt buộc cho Jetpack Compose Card/Box
        MapView(context, options).apply {
            onCreate(null)

            addOnDidFailLoadingMapListener { errorMessage ->
                Log.w(TAG, "Map failed loading: $errorMessage, trying fallback OSM raster style")
                mapLibreMap?.setStyle(Style.Builder().fromJson(OSM_RASTER_STYLE_JSON)) {
                    isStyleLoaded = true
                    Log.d(TAG, "Fallback OSM raster style loaded")
                }
            }

            getMapAsync { map ->
                mapLibreMap = map
                Log.d(TAG, "MapLibre MapAsync ready")

                // Cấu hình UI tối giản cho xe hơi
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isCompassEnabled = false

                applyMapStyle(map, isDarkMode)

                val initialPos = CameraPosition.Builder()
                    .target(LatLng(locationState.latitude, locationState.longitude))
                    .zoom(16.0)
                    .tilt(35.0)
                    .bearing(locationState.bearing.toDouble())
                    .build()
                map.cameraPosition = initialPos

                // Tạm dừng auto-follow khi tài xế vuốt bản đồ
                map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                    override fun onMoveBegin(detector: org.maplibre.android.gestures.MoveGestureDetector) {
                        onFollowCarChanged(false)
                    }
                    override fun onMove(detector: org.maplibre.android.gestures.MoveGestureDetector) {}
                    override fun onMoveEnd(detector: org.maplibre.android.gestures.MoveGestureDetector) {}
                })
            }
        }
    }

    // Đổi Style khi người dùng chuyển đổi Ngày / Đêm
    LaunchedEffect(isDarkMode) {
        val map = mapLibreMap
        if (map != null) {
            applyMapStyle(map, isDarkMode)
        }
    }

    // Quản lý Lifecycle đồng bộ ngay với trạng thái hiện tại của Activity
    DisposableEffect(lifecycleOwner, mapView) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Tự động bám theo tọa độ và góc xoay của xe
    LaunchedEffect(locationState.latitude, locationState.longitude, locationState.bearing, followCar, isStyleLoaded) {
        val map = mapLibreMap
        if (map != null && isStyleLoaded && followCar) {
            val targetPos = CameraPosition.Builder()
                .target(LatLng(locationState.latitude, locationState.longitude))
                .zoom(16.0)
                .tilt(35.0)
                .bearing(locationState.bearing.toDouble())
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(targetPos), 600)
        }
    }

    Box(modifier = modifier.background(if (isDarkMode) Color(0xFF121316) else Color(0xFFF2EFE9))) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Con trỏ xe điều hướng (Navigation Car Puck) hiển thị giữa màn hình khi followCar bật
        if (followCar && isStyleLoaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .graphicsLayer {
                        rotationZ = animatedBearing
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(34.dp)) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(size.width / 2f, size.height * 0.75f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, Color(0xFF1E88E5))
                    drawPath(path, Color.White, style = Stroke(width = 2.5.dp.toPx()))
                }
            }
        }

        // Chỉ báo GPS mất tín hiệu hoặc đang chờ tọa độ thực tế
        if (!locationState.hasGpsFix) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC000000))
            ) {
                Text(
                    text = "Đang tìm tín hiệu vệ tinh GPS...",
                    fontSize = 12.sp,
                    color = Color(0xFFFFB74D),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
