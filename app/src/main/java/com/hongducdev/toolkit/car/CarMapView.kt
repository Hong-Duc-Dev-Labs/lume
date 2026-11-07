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
import androidx.compose.runtime.rememberUpdatedState
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
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

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

private const val ROUTE_SOURCE_ID = "car-route-source"
private const val ROUTE_CASING_LAYER_ID = "car-route-casing"
private const val ROUTE_LINE_LAYER_ID = "car-route-line"

@Composable
fun CarMapView(
    locationState: CarLocationState,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    followCar: Boolean = true,
    isNavigating: Boolean = false,
    routeGeoJson: String? = null,
    destinationLatLng: LatLng? = null,
    recenterTrigger: Int = 0,
    onFollowCarChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFollowCarChanged by rememberUpdatedState(onFollowCarChanged)

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }
    var isFallbackActive by remember { mutableStateOf(false) }
    var isMapDestroyed by remember { mutableStateOf(false) }

    fun updateRouteOnMap(map: MapLibreMap, geoJson: String?) {
        val style = map.style ?: return
        try {
            var source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
            if (source == null) {
                source = GeoJsonSource(ROUTE_SOURCE_ID)
                style.addSource(source)

                val casing = LineLayer(ROUTE_CASING_LAYER_ID, ROUTE_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.lineColor(android.graphics.Color.parseColor("#0077FF")),
                        PropertyFactory.lineWidth(9.5f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineOpacity(0.85f)
                    )
                }
                style.addLayer(casing)

                val line = LineLayer(ROUTE_LINE_LAYER_ID, ROUTE_SOURCE_ID).apply {
                    setProperties(
                        PropertyFactory.lineColor(android.graphics.Color.parseColor("#00E5FF")),
                        PropertyFactory.lineWidth(5.5f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                    )
                }
                style.addLayer(line)
            }

            if (!geoJson.isNullOrEmpty()) {
                source.setGeoJson(geoJson)
            } else {
                source.setGeoJson("{\"type\":\"FeatureCollection\",\"features\":[]}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating route layer", e)
        }
    }

    fun applyMapStyle(map: MapLibreMap, dark: Boolean) {
        isStyleLoaded = false
        val styleUrl = if (dark) OFM_DARK_STYLE_URL else OFM_LIBERTY_STYLE_URL
        Log.d(TAG, "Applying style: $styleUrl")
        map.setStyle(Style.Builder().fromUri(styleUrl)) { _ ->
            isStyleLoaded = true
            isFallbackActive = false
            Log.d(TAG, "Style loaded successfully from $styleUrl")
            // Khôi phục route layer nếu đang có route
            if (!routeGeoJson.isNullOrEmpty()) {
                updateRouteOnMap(map, routeGeoJson)
            }
        }
    }

    val mapView = remember {
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true) // Bắt buộc cho Jetpack Compose Card/Box
        MapView(context, options).apply {
            onCreate(null)

            addOnDidFailLoadingMapListener { errorMessage ->
                if (isFallbackActive) return@addOnDidFailLoadingMapListener
                isFallbackActive = true
                Log.w(TAG, "Map failed loading: $errorMessage, trying fallback OSM raster style")
                mapLibreMap?.setStyle(Style.Builder().fromJson(OSM_RASTER_STYLE_JSON)) {
                    isStyleLoaded = true
                    Log.d(TAG, "Fallback OSM raster style loaded")
                    if (!routeGeoJson.isNullOrEmpty()) {
                        mapLibreMap?.let { updateRouteOnMap(it, routeGeoJson) }
                    }
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
                    .zoom(if (isNavigating) 17.5 else 16.0)
                    .tilt(if (isNavigating) 50.0 else 35.0)
                    .bearing(locationState.bearing.toDouble())
                    .build()
                map.cameraPosition = initialPos

                // Bắt TOÀN BỘ cử chỉ của người dùng (kéo, zoom 2 ngón, xoay 2 ngón) để tạm dừng auto-follow
                map.addOnCameraMoveStartedListener { reason ->
                    if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                        currentOnFollowCarChanged(false)
                    }
                }
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

    // Cập nhật tuyến đường khi routeGeoJson thay đổi
    LaunchedEffect(routeGeoJson, isStyleLoaded) {
        val map = mapLibreMap
        if (map != null && isStyleLoaded) {
            updateRouteOnMap(map, routeGeoJson)
        }
    }

    // Tự động thu phóng để thấy toàn bộ lộ trình khi có điểm đến mới (khi chưa bấm Bắt đầu dẫn đường)
    LaunchedEffect(destinationLatLng, routeGeoJson) {
        val map = mapLibreMap
        if (map != null && isStyleLoaded && destinationLatLng != null && !routeGeoJson.isNullOrEmpty() && !isNavigating) {
            try {
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(locationState.latitude, locationState.longitude))
                    .include(destinationLatLng)
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 1000)
                currentOnFollowCarChanged(false) // Tạm nhường camera cho toàn bộ lộ trình
            } catch (e: Exception) {
                Log.e(TAG, "Failed animating camera to route bounds", e)
            }
        }
    }

    // Quản lý Lifecycle đồng bộ an toàn: ngăn chặn double destroy gây SIGSEGV
    DisposableEffect(lifecycleOwner, mapView) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }

        val observer = LifecycleEventObserver { _, event ->
            if (isMapDestroyed) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    isMapDestroyed = true
                    mapView.onDestroy()
                    mapLibreMap = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!isMapDestroyed) {
                isMapDestroyed = true
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
                mapLibreMap = null
            }
        }
    }

    // Tự động bám theo tọa độ và góc xoay của xe (hoặc cưỡng chế quay lại vị trí xe khi bấm Định vị xe)
    LaunchedEffect(locationState.latitude, locationState.longitude, locationState.bearing, followCar, isStyleLoaded, isNavigating, recenterTrigger) {
        val map = mapLibreMap
        if (map != null && isStyleLoaded && followCar) {
            val targetPos = CameraPosition.Builder()
                .target(LatLng(locationState.latitude, locationState.longitude))
                .zoom(if (isNavigating) 17.5 else 16.0)
                .tilt(if (isNavigating) 50.0 else 35.0)
                .bearing(locationState.bearing.toDouble())
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(targetPos), if (isNavigating) 500 else 600)
        }
    }

    Box(modifier = modifier.background(if (isDarkMode) Color(0xFF121316) else Color(0xFFF2EFE9))) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Con trỏ xe điều hướng (Navigation Car Puck) hiển thị giữa màn hình khi followCar bật.
        // Vì camera MapLibre đã xoay theo bearing của xe nên con trỏ luôn chỉ thẳng về phía trước màn hình (0f).
        if (followCar && isStyleLoaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .graphicsLayer {
                        rotationZ = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(34.dp)) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 2f)
                        lineTo(size.width - 4f, size.height - 4f)
                        lineTo(size.width / 2f, size.height * 0.72f)
                        lineTo(4f, size.height - 4f)
                        close()
                    }
                    // Viền đậm tăng độ tương phản trên mọi loại địa hình bản đồ
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Màu xanh Neon dẫn đường nổi bật
                    drawPath(
                        path = path,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}
