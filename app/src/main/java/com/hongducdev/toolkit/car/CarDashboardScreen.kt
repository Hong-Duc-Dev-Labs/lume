package com.hongducdev.toolkit.car

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

/**
 * Giao diện Coolwalk Car Dashboard đẳng cấp (Modern Dark Automotive HUD):
 * - Hỗ trợ cả Landscape (ngang xe hơi) lẫn Portrait (dọc điện thoại).
 * - Giữ sáng màn hình khi đang ở chế độ lái xe (FLAG_KEEP_SCREEN_ON).
 * - Bản đồ MapLibre tối ưu hiển thị đường sá Việt Nam (chế độ Ngày/Đêm).
 * - Cụm HUD tốc độ kính mờ (Frosted Glass HUD) với thước đo tỷ lệ tốc độ.
 * - Biển báo giới hạn tốc độ chuẩn QCVN 41:2019 và hiệu ứng vầng sáng cảnh báo quá tốc độ.
 * - Thẻ cảnh báo biển báo/camera thông minh dạng viên thuốc phát sáng.
 * - CHỌN ĐỊA ĐIỂM & CHỈ ĐƯỜNG:
 *     + Tìm kiếm địa danh bằng Photon / OpenStreetMap Geocoder (tiếng Việt).
 *     + Tính toán lộ trình lái xe OSRM, vẽ dải đường phát sáng xanh Cyan trên bản đồ.
 *     + Thẻ xem trước lộ trình (Khoảng cách km, Thời gian dự kiến phút/giờ).
 *     + Nút kích hoạt Google Maps Navigation Turn-by-Turn chuyên sâu có giọng nói.
 * - Cụm Widget media cảm ứng lớn phong cách Audiophile Modern Luxury với:
 *     + Đĩa than Vinyl xoay 360° tự nhiên khi phát nhạc, trượt ra từ bìa album.
 *     + Dải quang phổ âm thanh đa băng tần (Audiophile Waveform Visualizer).
 *     + Thanh tiến trình phát nhạc phát sáng với thời lượng bài hát.
 *     + Huy hiệu nguồn phát (Spotify, YouTube Music) và định dạng Hi-Res Audio.
 * - Dock bar điều khiển sang trọng với icon vector chuẩn Material Icons.
 */
@Composable
fun CarDashboardScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()

    // Giữ màn hình luôn sáng trong suốt hành trình lái xe
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Quản lý định vị GPS
    val locationManager = remember { CarLocationManager(context) }
    val locationState by locationManager.locationState.collectAsState()

    // Quản lý Cảnh báo tốc độ & Biển báo giao thông
    val alertManager = remember { CarSpeedAlertManager(context) }
    val alertState by alertManager.alertState.collectAsState()

    // Quản lý Lộ trình & Tìm kiếm điểm đến
    val routingManager = remember { CarRoutingManager(context) }
    val activeRoute by routingManager.activeRoute.collectAsState()
    val searchResults by routingManager.searchResults.collectAsState()
    val isSearching by routingManager.isSearching.collectAsState()
    val isLoadingRoute by routingManager.isLoadingRoute.collectAsState()

    // Trạng thái hiển thị hộp thoại tìm kiếm
    var isSearchDialogOpen by remember { mutableStateOf(false) }

    // Chế độ mô phỏng tốc độ khi test trên bàn (chạm vào ô tốc độ để test)
    var simulatedSpeed by remember { mutableStateOf<Int?>(null) }
    val displaySpeed = simulatedSpeed ?: locationState.speedKmh

    // Đồng bộ tốc độ với engine cảnh báo
    LaunchedEffect(displaySpeed) {
        alertManager.onSpeedChanged(displaySpeed)
    }

    DisposableEffect(alertManager) {
        onDispose {
            alertManager.cleanup()
        }
    }

    // Quản lý phát nhạc qua NotificationListener
    val mediaState by CarNotificationListenerService.mediaState.collectAsState()

    // Trạng thái theo dõi xe
    var followCar by remember { mutableStateOf(true) }

    // Chế độ bản đồ: Ngày (Liberty rực rỡ, chi tiết) vs Đêm (Dark dịu mắt)
    var isDarkMode by remember { mutableStateOf(false) }

    // Đồng hồ thời gian thực
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, dd/MM", Locale.forLanguageTag("vi-VN"))
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000L)
        }
    }

    // Kiểm tra & xin quyền vị trí
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locationManager.startTracking()
        }
    }

    LaunchedEffect(Unit) {
        CarNotificationListenerService.checkPermissionStatus(context)
        val hasLoc = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasLoc) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            locationManager.startTracking()
        }
    }

    DisposableEffect(locationManager) {
        onDispose {
            locationManager.stopTracking()
        }
    }

    val onToggleSimulatedSpeed = {
        simulatedSpeed = when (simulatedSpeed) {
            null -> 45
            45 -> 75 // Vượt ngưỡng 60 -> kích hoạt cảnh báo quá tốc độ
            75 -> 95
            else -> null // Quay về GPS thực tế
        }
    }

    // Tọa độ đích cho camera MapLibre
    val destinationLatLng = activeRoute?.let { LatLng(it.destinationLat, it.destinationLng) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0A0B0E)
    ) {
        if (isLandscape) {
            Row(Modifier.fillMaxSize()) {
                // Dock Bar bên trái (Coolwalk Navigation Bar)
                CarSideDock(
                    timeString = currentTimeString,
                    dateString = currentDateString,
                    onExit = onExit,
                    onOpenMaps = { isSearchDialogOpen = true },
                    onOpenDialer = { openDialer(context) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(76.dp)
                )

                // Nội dung chính: Bản đồ chiếm 58%, Nhạc chiếm 42% (Audiophile layout)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CarMapCard(
                        locationState = locationState,
                        displaySpeed = displaySpeed,
                        alertState = alertState,
                        followCar = followCar,
                        isDarkMode = isDarkMode,
                        activeRoute = activeRoute,
                        routeGeoJson = activeRoute?.geoJson,
                        destinationLatLng = destinationLatLng,
                        isLoadingRoute = isLoadingRoute,
                        onOpenSearch = { isSearchDialogOpen = true },
                        onStartNavigation = {
                            activeRoute?.let { route ->
                                routingManager.startGoogleNavigation(
                                    route.destinationLat,
                                    route.destinationLng,
                                    route.destinationName
                                )
                            }
                        },
                        onClearRoute = {
                            routingManager.clearRoute()
                            followCar = true
                        },
                        onToggleDarkMode = { isDarkMode = !isDarkMode },
                        onToggleVoiceAlert = { alertManager.toggleVoiceAlert() },
                        onCycleSpeedLimit = { alertManager.cycleSpeedLimit() },
                        onCycleApproachingSign = { alertManager.cycleApproachingSign() },
                        onSimulateSpeed = onToggleSimulatedSpeed,
                        onFollowCarChanged = { followCar = it },
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    )

                    CarMediaCard(
                        mediaState = mediaState,
                        onOpenPermissionSettings = { openNotificationListenerSettings(context) },
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                    )
                }
            }
        } else {
            // Màn hình dọc (Portrait mode)
            Column(Modifier.fillMaxSize()) {
                CarMapCard(
                    locationState = locationState,
                    displaySpeed = displaySpeed,
                    alertState = alertState,
                    followCar = followCar,
                    isDarkMode = isDarkMode,
                    activeRoute = activeRoute,
                    routeGeoJson = activeRoute?.geoJson,
                    destinationLatLng = destinationLatLng,
                    isLoadingRoute = isLoadingRoute,
                    onOpenSearch = { isSearchDialogOpen = true },
                    onStartNavigation = {
                        activeRoute?.let { route ->
                            routingManager.startGoogleNavigation(
                                route.destinationLat,
                                route.destinationLng,
                                route.destinationName
                            )
                        }
                    },
                    onClearRoute = {
                        routingManager.clearRoute()
                        followCar = true
                    },
                    onToggleDarkMode = { isDarkMode = !isDarkMode },
                    onToggleVoiceAlert = { alertManager.toggleVoiceAlert() },
                    onCycleSpeedLimit = { alertManager.cycleSpeedLimit() },
                    onCycleApproachingSign = { alertManager.cycleApproachingSign() },
                    onSimulateSpeed = onToggleSimulatedSpeed,
                    onFollowCarChanged = { followCar = it },
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                CarMediaCard(
                    mediaState = mediaState,
                    onOpenPermissionSettings = { openNotificationListenerSettings(context) },
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                CarBottomDock(
                    timeString = currentTimeString,
                    dateString = currentDateString,
                    onExit = onExit,
                    onOpenMaps = { isSearchDialogOpen = true },
                    onOpenDialer = { openDialer(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(8.dp)
                )
            }
        }

        // HỘP THOẠI TÌM KIẾM ĐỊA ĐIỂM CHỈ ĐƯỜNG (DESTINATION SEARCH DIALOG)
        if (isSearchDialogOpen) {
            DestinationSearchDialog(
                userLat = locationState.latitude,
                userLng = locationState.longitude,
                searchResults = searchResults,
                isSearching = isSearching,
                onSearch = { query ->
                    coroutineScope.launch {
                        routingManager.searchPlaces(query, locationState.latitude, locationState.longitude)
                    }
                },
                onSelectDestination = { dest ->
                    isSearchDialogOpen = false
                    coroutineScope.launch {
                        routingManager.calculateRoute(
                            startLat = locationState.latitude,
                            startLng = locationState.longitude,
                            destination = dest
                        )
                    }
                },
                onDismiss = { isSearchDialogOpen = false }
            )
        }
    }
}

@Composable
private fun CarMapCard(
    locationState: CarLocationState,
    displaySpeed: Int,
    alertState: CarSpeedAlertState,
    followCar: Boolean,
    isDarkMode: Boolean,
    activeRoute: RouteInfo?,
    routeGeoJson: String?,
    destinationLatLng: LatLng?,
    isLoadingRoute: Boolean,
    onOpenSearch: () -> Unit,
    onStartNavigation: () -> Unit,
    onClearRoute: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleVoiceAlert: () -> Unit,
    onCycleSpeedLimit: () -> Unit,
    onCycleApproachingSign: () -> Unit,
    onSimulateSpeed: () -> Unit,
    onFollowCarChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(10.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14161D)),
        border = BorderStroke(1.dp, Color(0x1FFFFFFF))
    ) {
        Box(Modifier.fillMaxSize()) {
            CarMapView(
                locationState = locationState,
                isDarkMode = isDarkMode,
                followCar = followCar,
                routeGeoJson = routeGeoJson,
                destinationLatLng = destinationLatLng,
                onFollowCarChanged = onFollowCarChanged,
                modifier = Modifier.fillMaxSize()
            )

            // Cụm HUD Tốc độ + Biển báo giới hạn tốc độ + Cảnh báo tiếp cận
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // HUD Tốc độ kỹ thuật số phong cách ô tô sang trọng
                    val hudBgColor by animateColorAsState(
                        targetValue = if (alertState.isOverSpeed) Color(0xE6B71C1C) else Color(0xD9101218),
                        animationSpec = tween(250),
                        label = "hudBgColor"
                    )

                    Card(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSimulateSpeed() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = hudBgColor),
                        border = BorderStroke(
                            1.5.dp,
                            if (alertState.isOverSpeed) Color(0xFFFF5252) else Color(0x33FFFFFF)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "$displaySpeed",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (alertState.isOverSpeed) Color(0xFFFFEBEE) else Color.White
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "km/h",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alertState.isOverSpeed) Color(0xFFFFCDD2) else Color(0xFF00E676),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            // Thanh đo tỷ lệ tốc độ / giới hạn (Speed ratio track)
                            val speedRatio = (displaySpeed.toFloat() / alertState.speedLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1.25f)
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = (speedRatio / 1.25f).coerceIn(0.05f, 1f))
                                        .background(
                                            if (alertState.isOverSpeed) Color(0xFFFF1744)
                                            else if (speedRatio > 0.85f) Color(0xFFFFB300)
                                            else Color(0xFF00E676)
                                        )
                                )
                            }
                        }
                    }

                    // Biển báo giới hạn tốc độ (QCVN 41:2019 - Biển P.127)
                    SpeedLimitSign(
                        speedLimit = alertState.speedLimit,
                        isOverSpeed = alertState.isOverSpeed,
                        onClick = onCycleSpeedLimit
                    )
                }

                // Huy hiệu cảnh báo phụ (Camera phạt nguội, Khu dân cư...)
                alertState.approachingSign?.let { sign ->
                    ApproachingSignBadge(
                        sign = sign,
                        onClick = onCycleApproachingSign
                    )
                }
            }

            // Nút điều khiển nhanh bên phải: Định tâm, Giọng nói, Sáng/Tối, Tìm đường
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopEnd),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CockpitActionButton(
                    icon = Icons.Rounded.MyLocation,
                    isActive = followCar,
                    activeColor = Color(0xFF2979FF),
                    onClick = { onFollowCarChanged(true) }
                )

                CockpitActionButton(
                    icon = if (alertState.isVoiceAlertEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    isActive = alertState.isVoiceAlertEnabled,
                    activeColor = Color(0xFF00C853),
                    onClick = onToggleVoiceAlert
                )

                CockpitActionButton(
                    icon = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    isActive = isDarkMode,
                    activeColor = Color(0xFFFFB300),
                    onClick = onToggleDarkMode
                )

                CockpitActionButton(
                    icon = Icons.Rounded.Search,
                    isActive = activeRoute != null,
                    activeColor = Color(0xFF00E5FF),
                    onClick = onOpenSearch
                )
            }

            // VÙNG ĐIỀU KHIỂN ĐIỂM ĐẾN PHÍA DƯỚI BẢN ĐỒ (BOTTOM ROUTE / SEARCH)
            if (activeRoute == null) {
                // Thanh gõ tìm kiếm kính mờ (Search Pill)
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .clickable { onOpenSearch() }
                        .shadow(8.dp, RoundedCornerShape(30.dp)),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xD910131E)),
                    border = BorderStroke(1.dp, Color(0x332979FF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF2979FF),
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = "Tìm điểm đến chỉ đường...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0BEC5)
                        )

                        Spacer(Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0x3300E676)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NearMe,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                // Thẻ xem trước lộ trình (Floating Route Summary Card)
                FloatingRoutePreviewCard(
                    route = activeRoute,
                    onStartNavigation = onStartNavigation,
                    onCancelRoute = onClearRoute,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }

            // Chỉ báo khi đang tính toán lộ trình OSRM
            if (isLoadingRoute) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .shadow(12.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xE6101420)),
                    border = BorderStroke(1.dp, Color(0x4D00E5FF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF00E5FF),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Đang vẽ lộ trình xe hơi...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * THẺ XEM TRƯỚC LỘ TRÌNH (FLOATING ROUTE PREVIEW CARD):
 * Hiển thị điểm đến, khoảng cách km, thời gian ước tính, nút Bắt đầu chỉ đường Google Maps & Hủy
 */
@Composable
private fun FloatingRoutePreviewCard(
    route: RouteInfo,
    onStartNavigation: () -> Unit,
    onCancelRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .shadow(14.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEB0F1422)),
        border = BorderStroke(1.2.dp, Color(0x6600E5FF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tên điểm đến & Nút đóng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = route.destinationName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onCancelRoute,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFF90A4AE),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Thông số Khoảng cách + Thời gian + Nút Bắt đầu dẫn đường
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Khoảng cách
                    Column {
                        Text(
                            text = "KHOẢNG CÁCH",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78909C)
                        )
                        Text(
                            text = route.distanceDisplay,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Thời gian ước tính
                    Column {
                        Text(
                            text = "THỜI GIAN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78909C)
                        )
                        Text(
                            text = route.durationDisplay,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E676)
                        )
                    }
                }

                // Nút Bắt đầu chỉ đường Google Maps
                Button(
                    onClick = onStartNavigation,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2979FF)
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Bắt đầu",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * HỘP THOẠI TÌM KIẾM ĐỊA ĐIỂM CHUẨN COCKPIT
 */
@Composable
private fun DestinationSearchDialog(
    userLat: Double,
    userLng: Double,
    searchResults: List<DestinationItem>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onSelectDestination: (DestinationItem) -> Unit,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .shadow(20.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10131C)),
            border = BorderStroke(1.dp, Color(0x332979FF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hàng tiêu đề & Ô nhập tìm kiếm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = {
                            queryText = it
                            if (it.length >= 2) {
                                onSearch(it)
                            }
                        },
                        placeholder = {
                            Text(
                                "Nhập địa chỉ, tên đường, sân bay...",
                                fontSize = 13.sp,
                                color = Color(0xFF78909C)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Color(0xFF2979FF)
                            )
                        },
                        trailingIcon = {
                            if (queryText.isNotEmpty()) {
                                IconButton(onClick = { queryText = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF90A4AE)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2979FF),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF2979FF)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x26FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // HÀNG PHÍM CHỌN NHANH (QUICK SHORTCUT CHIPS)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CarRoutingManager.QUICK_SEARCH_CHIPS.forEach { chipLabel ->
                        val chipIcon = when {
                            chipLabel.contains("xăng") -> Icons.Rounded.LocalGasStation
                            chipLabel.contains("xe") -> Icons.Rounded.LocalParking
                            chipLabel.contains("viện") -> Icons.Rounded.LocalHospital
                            else -> Icons.Rounded.DirectionsCar
                        }

                        Card(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    queryText = chipLabel
                                    onSearch(chipLabel)
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1F2979FF)),
                            border = BorderStroke(1.dp, Color(0x4D2979FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = chipIcon,
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = chipLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE3F2FD)
                                )
                            }
                        }
                    }
                }

                // TRẠNG THÁI TÌM KIẾM & DANH SÁCH KẾT QUẢ
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2979FF),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (searchResults.isEmpty() && queryText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy địa điểm phù hợp",
                            fontSize = 13.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectDestination(item) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                                border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x332979FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Place,
                                            contentDescription = null,
                                            tint = Color(0xFF64B5F6),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.subtitle.isNotEmpty()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = item.subtitle,
                                                fontSize = 11.sp,
                                                color = Color(0xFF90A4AE),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Nút hành động chuẩn Cockpit: Phản hồi thị giác xúc giác mượt mà
 */
@Composable
private fun CockpitActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) activeColor.copy(alpha = 0.25f) else Color(0xCC13151D)
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) activeColor else Color(0x2BFFFFFF)
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else Color(0xFFD0D4DC),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Biển báo tốc độ chuẩn Việt Nam QCVN 41:2019 (Biển P.127) với vòng aura nhấp nháy khi quá tốc độ
 */
@Composable
fun SpeedLimitSign(
    speedLimit: Int,
    isOverSpeed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseHalo by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseHalo"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .then(
                if (isOverSpeed) {
                    Modifier.border(pulseHalo.dp, Color(0x80FF1744), CircleShape)
                } else {
                    Modifier
                }
            )
            .padding(if (isOverSpeed) 2.dp else 0.dp)
            .shadow(6.dp, CircleShape)
            .background(Color.White)
            .border(5.5.dp, Color(0xFFD32F2F), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$speedLimit",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = Color(0xFF1A1A1A)
        )
    }
}

/**
 * Huy hiệu cảnh báo tiếp cận chuẩn phong cách Glassmorphism
 */
@Composable
fun ApproachingSignBadge(
    sign: RoadSign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, badgeColor) = when (sign.type) {
        SignType.SPEED_CAMERA -> Icons.Rounded.PhotoCamera to Color(0xFFFF9100)
        SignType.RESIDENTIAL_AREA -> Icons.Rounded.LocationCity to Color(0xFF00E5FF)
        SignType.END_RESIDENTIAL -> Icons.Rounded.HomeWork to Color(0xFF00E676)
        SignType.SPEED_LIMIT -> Icons.Rounded.Warning to Color(0xFFFFD600)
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE6141620)),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = sign.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            if (sign.distanceMeters > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${sign.distanceMeters}m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}

/**
 * THẺ MEDIA AUDIOPHILE MODERN LUXURY:
 * - Đĩa than Vinyl quay 360° trượt mượt mà ra từ bao đĩa (Album Jacket).
 * - Dải quang phổ âm thanh 18 băng tần sống động (Audiophile Spectrum Visualizer).
 * - Thanh tiến trình phát nhạc phát sáng neon với bộ đếm thời gian.
 * - Huy hiệu Hi-Res Audio / Dolby Atmos & Nguồn phát thông minh (Spotify, YouTube Music...).
 * - Bộ phím điều khiển buồng lái cảm ứng sang trọng, xúc giác chân thực.
 */
@Composable
private fun CarMediaCard(
    mediaState: CarMediaState,
    onOpenPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Trạng thái phụ: Shuffle & Favorite
    var isShuffle by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }

    // Quản lý mô phỏng tiến trình phát nhạc nếu hệ thống chưa trả duration/position
    var simulatedPositionMs by remember { mutableLongStateOf(84000L) } // 01:24
    val simulatedDurationMs = 218000L // 03:38

    LaunchedEffect(mediaState.isPlaying) {
        if (mediaState.isPlaying) {
            while (true) {
                delay(1000L)
                simulatedPositionMs = (simulatedPositionMs + 1000L) % simulatedDurationMs
            }
        }
    }

    val displayPos = if (mediaState.positionMs > 0) mediaState.positionMs else simulatedPositionMs
    val displayDur = if (mediaState.durationMs > 0) mediaState.durationMs else simulatedDurationMs

    // Xác định màu sắc chủ đạo theo ứng dụng nguồn
    val brandColor = when {
        mediaState.appName.contains("Spotify", ignoreCase = true) -> Color(0xFF1DB954)
        mediaState.appName.contains("YouTube", ignoreCase = true) -> Color(0xFFFF0033)
        mediaState.appName.contains("Apple", ignoreCase = true) -> Color(0xFFFC3C44)
        mediaState.appName.contains("Zing", ignoreCase = true) -> Color(0xFF6F32E2)
        else -> Color(0xFF388E3C)
    }

    Card(
        modifier = modifier.shadow(12.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
        border = BorderStroke(1.dp, Color(0x24FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            brandColor.copy(alpha = if (mediaState.isPlaying) 0.18f else 0.08f),
                            Color(0x331E2230),
                            Color(0xFF0F1118)
                        ),
                        center = Offset(200f, 200f),
                        radius = 800f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // HÀNG 1: NGUỒN PHÁT + HUY HIỆU HI-RES AUDIO / DOLBY ATMOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nguồn phát nhạc (App source pill)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (mediaState.isPlaying) brandColor else Color(0xFF757575))
                        )
                        Text(
                            text = if (mediaState.isPlaying) mediaState.appName.ifEmpty { "Đang phát" } else "Trình phát",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    // Huy hiệu Hi-Res Audio cao cấp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0x66FFD700), RoundedCornerShape(6.dp))
                                .background(Color(0x1AFFD700))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HI-RES AUDIO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD54F),
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (!mediaState.isNotificationListenerEnabled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x332979FF))
                                    .clickable { onOpenPermissionSettings() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Cấp quyền",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6)
                                )
                            }
                        }
                    }
                }

                // HÀNG 2: ĐĨA THAN VINYL XOAY + BAO ĐĨA ALBUM SANG TRỌNG
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    VinylTurntableArt(
                        albumArt = mediaState.albumArt,
                        isPlaying = mediaState.isPlaying,
                        accentColor = brandColor
                    )
                }

                // HÀNG 3: THÔNG TIN BÀI HÁT & CA SĨ
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mediaState.title.ifEmpty { "Chưa phát bài hát nào" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = mediaState.artist.ifEmpty { "Mở Spotify, Zing MP3 hoặc YouTube Music" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // HÀNG 4: QUANG PHỔ ÂM THANH AUDIOPHILE (SPECTRUM WAVEFORM)
                AudiophileSpectrumVisualizer(
                    isPlaying = mediaState.isPlaying,
                    tintColor = brandColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )

                // HÀNG 5: THANH TIẾN TRÌNH TRACK PHÁT NHẠC
                MediaTrackProgressBar(
                    currentMs = displayPos,
                    totalMs = displayDur,
                    accentColor = brandColor,
                    onSeek = { seekPos ->
                        simulatedPositionMs = seekPos
                        CarNotificationListenerService.seekTo(seekPos)
                    }
                )

                // HÀNG 6: BỘ PHÍM ĐIỀU KHIỂN BUỒNG LÁI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Shuffle
                    IconButton(
                        onClick = { isShuffle = !isShuffle },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) brandColor else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Nút Lùi bài (Previous)
                    IconButton(
                        onClick = { CarNotificationListenerService.skipToPrevious() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x1FFFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Nút Play / Pause Trung tâm
                    Card(
                        modifier = Modifier
                            .size(58.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                if (mediaState.isPlaying) {
                                    CarNotificationListenerService.pause()
                                } else {
                                    CarNotificationListenerService.play()
                                }
                            },
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            brandColor,
                                            brandColor.copy(alpha = 0.75f),
                                            Color(0xFF0F172A)
                                        )
                                    )
                                )
                                .border(1.5.dp, Color(0x66FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Nút Qua bài (Next)
                    IconButton(
                        onClick = { CarNotificationListenerService.skipToNext() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x1FFFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Nút Yêu thích (Favorite Heart)
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF1744) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * ĐĨA THAN VINYL XOAY 360° KÈM BAO BÌA SANG TRỌNG
 */
@Composable
private fun VinylTurntableArt(
    albumArt: Bitmap?,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .width(180.dp)
            .height(116.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. ĐĨA THAN VINYL (Nằm phía sau bên phải, xoay tròn khi đang phát)
        Box(
            modifier = Modifier
                .size(108.dp)
                .offset(x = 64.dp)
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotationAngle else 0f
                }
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF0F1014)),
            contentAlignment = Alignment.Center
        ) {
            // Vẽ các rãnh đĩa than đồng tâm và phản quang
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2

                // Rãnh đĩa ngoài
                drawCircle(
                    color = Color(0xFF1E212B),
                    radius = maxRadius - 2,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
                // Các rãnh âm thanh vi mô (Micro grooves)
                for (r in listOf(0.88f, 0.80f, 0.72f, 0.64f, 0.56f, 0.48f)) {
                    drawCircle(
                        color = Color(0x33FFFFFF),
                        radius = maxRadius * r,
                        center = center,
                        style = Stroke(width = 0.8f)
                    )
                }

                // Vành nhãn trung tâm
                drawCircle(
                    color = Color(0xFF2B3040),
                    radius = maxRadius * 0.36f,
                    center = center
                )

                // Lỗ trục giữa (Spindle hole)
                drawCircle(
                    color = Color(0xFF0A0A0C),
                    radius = 7f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFB0BEC5),
                    radius = 9f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // Tem nhãn trung tâm (Center label)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                accentColor,
                                Color(0xFF6366F1),
                                accentColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0C))
                )
            }
        }

        // 2. BAO BÌA ALBUM (Nằm phía trước bên trái với bóng đổ nổi bật)
        Box(
            modifier = Modifier
                .size(112.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1F2B))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF252B3B), Color(0xFF131620))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            // Hiệu ứng ánh sáng bóng trên bao bìa (Glossy reflection highlight)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0x2BFFFFFF),
                                Color(0x00FFFFFF)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(100f, 100f)
                        )
                    )
            )
        }
    }
}

/**
 * QUANG PHỔ ÂM THANH ĐA BĂNG TẦN (20 SPECTRUM BARS)
 */
@Composable
private fun AudiophileSpectrumVisualizer(
    isPlaying: Boolean,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "spectrum")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        val totalBars = 20
        for (i in 0 until totalBars) {
            val wave = if (isPlaying) {
                val v = (sin((phase + i * 0.45f).toDouble()) * 0.5f + 0.5f).toFloat()
                (6f + v * 16f).coerceIn(4f, 22f)
            } else {
                3.5f
            }

            val barAlpha = 0.55f + (i.toFloat() / totalBars) * 0.45f

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(wave.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                tintColor.copy(alpha = barAlpha),
                                Color(0xFF60A5FA).copy(alpha = barAlpha * 0.7f)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * THANH TIẾN TRÌNH TRACK PHÁT NHẠC VỚI BỘ ĐẾM THỜI GIAN
 */
@Composable
private fun MediaTrackProgressBar(
    currentMs: Long,
    totalMs: Long,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalMs > 0) {
        (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0.35f
    }

    val formatTime = { ms: Long ->
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format(Locale.US, "%02d:%02d", min, sec)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x33FFFFFF))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, Color(0xFF60A5FA))
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.CenterEnd)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, accentColor, CircleShape)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentMs),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = formatTime(totalMs),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun CarSideDock(
    timeString: String,
    dateString: String,
    onExit: () -> Unit,
    onOpenMaps: () -> Unit,
    onOpenDialer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151D)),
        border = BorderStroke(1.dp, Color(0x1FFFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Giờ hiện tại
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeString,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White
                )
                Text(
                    text = dateString,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8A90A2)
                )
            }

            // Các App Icons nhanh
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onOpenMaps,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF2979FF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onOpenDialer,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Dialer",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Nút Thoát Dashboard về Toolkit chính
            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x33D32F2F))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = "Home",
                    tint = Color(0xFFFF8A80),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CarBottomDock(
    timeString: String,
    dateString: String,
    onExit: () -> Unit,
    onOpenMaps: () -> Unit,
    onOpenDialer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151D)),
        border = BorderStroke(1.dp, Color(0x1FFFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeString,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = Color(0xFF8A90A2)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = onOpenMaps) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF2979FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onOpenDialer) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Dialer",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onExit) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = "Exit",
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun openDialer(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

private fun openNotificationListenerSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}
