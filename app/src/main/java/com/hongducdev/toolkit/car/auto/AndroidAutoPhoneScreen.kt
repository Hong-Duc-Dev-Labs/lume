package com.hongducdev.toolkit.car.auto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hongducdev.toolkit.car.CarMapView
import com.hongducdev.toolkit.car.CarRoutingManager
import com.hongducdev.toolkit.car.DestinationItem
import com.hongducdev.toolkit.car.RoadSign
import com.hongducdev.toolkit.car.RouteInfo
import com.hongducdev.toolkit.car.SignType
import com.hongducdev.toolkit.ui.theme.ExpressiveRadius
import com.hongducdev.toolkit.utils.ColorUtil
import com.hongducdev.toolkit.utils.HapticUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

private val PermissionList = arrayOf(
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.ACCESS_COARSE_LOCATION
)

/** Các mức tốc độ mô phỏng khi chạm vào cụm HUD (không có GPS thật, ví dụ đang ở trong nhà). */
private val SimulatedSpeedCycle = listOf(45, 75, 95)

/**
 * Màn hình kiểu Android Auto chạy trực tiếp trên điện thoại: HUD tốc độ, biển báo,
 * đường đi, tìm đường và xem trước lộ trình. Dùng chung dịch vụ với bản trên xe
 * ([CarAppServices]) nên hành vi hai nơi giống nhau. Giao diện dùng chung màu sắc,
 * hình khối và kiểu chữ Material 3 Expressive của toàn app.
 */
@Composable
fun AndroidAutoPhoneScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val services = remember { CarAppServices(context.applicationContext) }
    DisposableEffect(services) { onDispose { services.close() } }

    val location by services.location.locationState.collectAsState()
    val alert by services.alerts.alertState.collectAsState()
    val route by services.routing.activeRoute.collectAsState()
    val results by services.routing.searchResults.collectAsState()
    val isSearching by services.routing.isSearching.collectAsState()
    val isLoadingRoute by services.routing.isLoadingRoute.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }
    var followCar by remember { mutableStateOf(true) }
    var recenterTrigger by remember { mutableStateOf(0) }
    var isNavigating by remember { mutableStateOf(false) }
    var routeJob by remember { mutableStateOf<Job?>(null) }
    val isDarkMode = isSystemInDarkTheme()
    val destinationLatLng = route?.let { LatLng(it.destinationLat, it.destinationLng) }

    LaunchedEffect(route) {
        if (route == null) isNavigating = false
    }

    BackHandler {
        when {
            searchOpen -> searchOpen = false
            isLoadingRoute -> {
                routeJob?.cancel()
                routeJob = null
                services.routing.clearRoute()
            }
            isNavigating -> isNavigating = false
            route != null -> services.routing.clearRoute()
            else -> onExit()
        }
    }

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) services.location.startTracking()
    }
    LaunchedEffect(Unit) {
        val granted = PermissionList.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) services.location.startTracking() else permissionLauncher.launch(PermissionList)
    }

    // Mô phỏng tốc độ khi test trong nhà (không có tín hiệu GPS): chạm vào cụm HUD để đổi mức.
    var simulatedSpeed by remember { mutableStateOf<Int?>(null) }
    val displaySpeed = simulatedSpeed ?: location.speedKmh
    val onToggleSimulation = {
        HapticUtil.performVirtualKeyHaptic(view)
        val currentIndex = SimulatedSpeedCycle.indexOf(simulatedSpeed)
        simulatedSpeed = if (currentIndex in 0 until SimulatedSpeedCycle.size - 1) {
            SimulatedSpeedCycle[currentIndex + 1]
        } else if (currentIndex == -1) {
            SimulatedSpeedCycle.first()
        } else {
            null
        }
    }

    LaunchedEffect(displaySpeed) { services.alerts.onSpeedChanged(displaySpeed) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            CarMapView(
                locationState = location,
                isDarkMode = isDarkMode,
                followCar = followCar,
                isNavigating = isNavigating,
                routeGeoJson = route?.geoJson,
                destinationLatLng = destinationLatLng,
                recenterTrigger = recenterTrigger,
                onFollowCarChanged = { followCar = it }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    SpeedPanel(
                        speed = displaySpeed,
                        limit = alert.speedLimit,
                        over = alert.isOverSpeed,
                        isSimulated = simulatedSpeed != null,
                        onClick = onToggleSimulation
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoundIconButton(
                                icon = Icons.Rounded.Close,
                                contentDescription = "Thoát",
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onExit()
                                }
                            )
                            RoundIconButton(
                                icon = if (alert.isVoiceAlertEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = "Bật/Tắt âm",
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    services.alerts.toggleVoiceAlert()
                                }
                            )
                            RoundIconButton(
                                icon = Icons.Rounded.MyLocation,
                                contentDescription = "Định vị xe",
                                tint = if (followCar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    followCar = true
                                    recenterTrigger++
                                }
                            )
                        }

                        SignPill(
                            sign = alert.approachingSign,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                services.alerts.cycleApproachingSign()
                            }
                        )
                    }
                }

                val current = route
                Box(Modifier.fillMaxWidth()) {
                    if (current == null) {
                        SearchPill {
                            HapticUtil.performVirtualKeyHaptic(view)
                            searchOpen = true
                        }
                    } else {
                        RouteCard(
                            route = current,
                            isNavigating = isNavigating,
                            onStart = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                followCar = true
                                recenterTrigger++
                                isNavigating = true
                            },
                            onStop = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                isNavigating = false
                                services.routing.clearRoute()
                            },
                            onOpenGoogleMaps = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                isNavigating = false
                                openGoogleMapsNavigation(context, current)
                            }
                        )
                    }
                }
            }

            if (isLoadingRoute) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(ExpressiveRadius.Card))
                        .padding(20.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            if (searchOpen) {
                SearchBottomSheet(
                    results = results,
                    isSearching = isSearching,
                    onSearch = { query ->
                        scope.launch { services.routing.searchPlaces(query, location.latitude, location.longitude) }
                    },
                    onSelect = { item ->
                        searchOpen = false
                        isNavigating = false
                        routeJob?.cancel()
                        routeJob = scope.launch {
                            services.routing.calculateRoute(location.latitude, location.longitude, item)
                        }
                    },
                    onDismiss = { searchOpen = false }
                )
            }
        }
    }
}

@Composable
private fun SpeedPanel(speed: Int, limit: Int, over: Boolean, isSimulated: Boolean, onClick: () -> Unit) {
    val ratio = (speed.toFloat() / limit.coerceAtLeast(1)).coerceIn(0f, 1.25f)
    val color = when {
        over -> MaterialTheme.colorScheme.error
        ratio > 0.85f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(ExpressiveRadius.Card))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(BorderStroke(2.dp, color), RoundedCornerShape(ExpressiveRadius.Card)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$speed",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = if (isSimulated) "km/h (Mô phỏng)" else "km/h",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SignPill(sign: RoadSign?, onClick: () -> Unit) {
    val bg = when {
        sign == null -> MaterialTheme.colorScheme.surfaceContainerHigh
        sign.type == SignType.SPEED_LIMIT -> MaterialTheme.colorScheme.errorContainer
        sign.type == SignType.SPEED_CAMERA -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val tint = when {
        sign == null -> MaterialTheme.colorScheme.onSurfaceVariant
        sign.type == SignType.SPEED_LIMIT -> MaterialTheme.colorScheme.error
        sign.type == SignType.SPEED_CAMERA -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (sign?.type) {
        SignType.SPEED_LIMIT -> Icons.Rounded.Warning
        SignType.SPEED_CAMERA -> Icons.Rounded.PhotoCamera
        SignType.RESIDENTIAL_AREA -> Icons.Rounded.HomeWork
        SignType.END_RESIDENTIAL -> Icons.Rounded.LocationCity
        else -> Icons.Rounded.Place
    }
    val title = sign?.title ?: "Chạm đổi biển báo test"
    val sub = if (sign != null && sign.distanceMeters > 0) "${sign.distanceMeters} m" else ""

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(ExpressiveRadius.Pill))
            .clickable(onClick = onClick)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (sub.isNotEmpty()) {
                Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ExpressiveRadius.Pill))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = "Tìm kiếm cây xăng, sân bay, bệnh viện...",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RouteCard(
    route: RouteInfo,
    isNavigating: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenGoogleMaps: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ExpressiveRadius.Card))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = route.destinationName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${route.distanceDisplay} • ${route.durationDisplay}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isNavigating) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(ExpressiveRadius.Pill))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ĐANG DẪN ĐƯỜNG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isNavigating) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(ExpressiveRadius.Pill)
                ) {
                    Icon(Icons.Rounded.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bắt đầu")
                }
            } else {
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(ExpressiveRadius.Pill)
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dừng lại")
                }
            }

            Button(
                onClick = onOpenGoogleMaps,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(ExpressiveRadius.Pill)
            ) {
                Icon(Icons.Rounded.Map, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Google Maps", color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBottomSheet(
    results: List<DestinationItem>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (DestinationItem) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    // Mặc định chọn ngay danh mục đầu tiên (Cây xăng) để màn hình luôn có nội dung tức thì khi vừa mở
    var selectedChip by remember { mutableStateOf<String?>(CarRoutingManager.QUICK_SEARCH_CHIPS.firstOrNull() ?: "Cây xăng") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Tự động kích hoạt tìm kiếm danh mục mặc định ngay khi vừa mở Bottom Sheet
    LaunchedEffect(Unit) {
        selectedChip?.let { onSearch(it) }
    }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            selectedChip = null
            delay(350)
            onSearch(trimmed)
        } else if (trimmed.isEmpty() && selectedChip != null) {
            onSearch(selectedChip!!)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Nhập địa chỉ hoặc tên địa điểm...") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                selectedChip = CarRoutingManager.QUICK_SEARCH_CHIPS.firstOrNull() ?: "Cây xăng"
                                selectedChip?.let { onSearch(it) }
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Xóa")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(ExpressiveRadius.Pill)
                )
            }

            // Gợi ý tìm nhanh theo danh mục (cây xăng, bãi đỗ xe, bệnh viện, sân bay...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarRoutingManager.QUICK_SEARCH_CHIPS.forEach { chip ->
                    val isSelected = (selectedChip == chip && query.isBlank()) || query.equals(chip, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(ExpressiveRadius.Pill))
                            .clickable {
                                selectedChip = chip
                                query = ""
                                onSearch(chip)
                            }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tiêu đề trạng thái gợi ý / kết quả tìm kiếm
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeTitle = when {
                    selectedChip != null -> "Gần bạn • $selectedChip"
                    query.isNotBlank() -> "Kết quả cho \"$query\""
                    else -> "Địa điểm xung quanh"
                }
                Text(
                    text = activeTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (results.isNotEmpty() && !isSearching) {
                    Text(
                        text = "${results.size} địa điểm",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Đang tìm kiếm ${selectedChip ?: query} gần bạn...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isSearching && results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy địa điểm phù hợp",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else if (!isSearching) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ExpressiveRadius.Card))
                                .clickable { onSelect(item) }
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(ExpressiveRadius.Pill))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.subtitle.isNotEmpty()) {
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                Icons.Rounded.Navigation,
                                contentDescription = "Chỉ đường",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

private fun openGoogleMapsNavigation(context: Context, route: RouteInfo) {
    val uri = Uri.parse("google.navigation:q=${route.destinationLat},${route.destinationLng}&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${route.destinationLat},${route.destinationLng}&travelmode=driving")
    val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            // Tránh crash nếu máy không có trình duyệt/maps
        }
    }
}
