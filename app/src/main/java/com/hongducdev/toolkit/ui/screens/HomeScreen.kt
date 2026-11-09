package com.hongducdev.toolkit.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hongducdev.toolkit.data.DisplayMode
import com.hongducdev.toolkit.data.FontSettings
import com.hongducdev.toolkit.data.FontSettingsRepository
import com.hongducdev.toolkit.data.LunarDisplayPolicy
import com.hongducdev.toolkit.data.LunarSettings
import com.hongducdev.toolkit.data.LunarSettingsRepository
import com.hongducdev.toolkit.data.ScreenTimeoutSettings
import com.hongducdev.toolkit.data.ScreenTimeoutSettingsRepository
import com.hongducdev.toolkit.ui.components.DeviceHeroCard
import com.hongducdev.toolkit.ui.components.PullTitleHeader
import com.hongducdev.toolkit.ui.components.TimeoutPresetPickerSheet
import com.hongducdev.toolkit.ui.components.rememberPullTitleHeaderState
import com.hongducdev.toolkit.ui.core.cards.FeatureCard
import com.hongducdev.toolkit.ui.core.cards.SettingsItem
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.ui.modifiers.scrollMotionBlur
import com.hongducdev.toolkit.wallpaper.LunarWallpaperService
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOpenLunar: () -> Unit,
    onOpenDiy: () -> Unit,
    onOpenCarDashboard: () -> Unit,
    onOpenFontSettings: () -> Unit,
    onOpenQrScanner: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { LunarSettingsRepository(context.applicationContext) }
    val settings by repository.settings.collectAsState(initial = LunarSettings())

    val fontRepository = remember { FontSettingsRepository(context.applicationContext) }
    val fontSettings by fontRepository.settings.collectAsState(initial = FontSettings())

    val screenTimeoutRepository = remember { ScreenTimeoutSettingsRepository(context.applicationContext) }
    val screenTimeoutSettings by screenTimeoutRepository.settings.collectAsState(initial = ScreenTimeoutSettings())
    val showLockscreenTimeoutPicker = remember { mutableStateOf(false) }
    val showScreenTimeoutPicker = remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val hookActive =
        LunarDisplayPolicy.isHookActive(
            lastSeenMillis = settings.hookLastSeen,
            bootMillis = now - SystemClock.elapsedRealtime(),
            nowMillis = now,
        )
    val wallpaperActive = LunarDisplayPolicy.wallpaperShouldShow(settings.displayMode, hookActive)

    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    val headerState = rememberPullTitleHeaderState()
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(headerState.nestedScrollConnection)
                .scrollMotionBlur(lazyListState),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = 150.dp,
                start = 16.dp,
                end = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PullTitleHeader(
                title = "Trang chủ",
                caption = "Galaxy S10 5G • Android 16 (AOSP)",
                state = headerState,
            )
        }

        // Device Specs Hero Card (Galaxy S10 5G / Beyondx, Android 16, RAM/Storage)
        item {
            DeviceHeroCard()
        }

        // Main Features Container
        item {
            RoundedCardContainer(title = "Tính năng chính") {
                FeatureCard(
                    title = "Lịch âm Việt Nam",
                    description = "Xem lịch âm cả tháng, tra cứu ngày tốt & hiển thị trên màn hình khóa",
                    icon = Icons.Rounded.CalendarMonth,
                    isEnabled = true,
                    onToggle = { onOpenLunar() },
                    onClick = onOpenLunar,
                    showToggle = false,
                )
                FeatureCard(
                    title = "Tự động hóa (DIY Rules)",
                    description = "Tạo các quy tắc tự động hóa theo sự kiện, sạc, pin, kết nối và ứng dụng",
                    icon = Icons.Rounded.AutoMode,
                    isEnabled = true,
                    onToggle = { onOpenDiy() },
                    onClick = onOpenDiy,
                    showToggle = false,
                )
                FeatureCard(
                    title = "Chế độ lái xe (Car Dashboard)",
                    description = "Bản đồ MapLibre, HUD đo tốc độ GPS, cảnh báo camera & tìm đường",
                    icon = Icons.Rounded.DirectionsCar,
                    isEnabled = hasLocationPermission,
                    onToggle = { onOpenCarDashboard() },
                    onClick = onOpenCarDashboard,
                    showToggle = false,
                )
                FeatureCard(
                    title = "Quét mã QR",
                    description = "Quét camera trực tiếp hoặc chọn ảnh từ thư viện (URL, Wi-Fi, văn bản)",
                    icon = Icons.Rounded.QrCodeScanner,
                    isEnabled = true,
                    onToggle = { onOpenQrScanner() },
                    onClick = onOpenQrScanner,
                    showToggle = false,
                )
                FeatureCard(
                    title = "Font UI hệ thống",
                    description = "Chọn file font .ttf / .otf tùy chỉnh làm phông chữ giao diện System UI",
                    icon = Icons.Rounded.TextFields,
                    isEnabled = fontSettings.isEnabled && fontSettings.fontName.isNotBlank(),
                    onToggle = { onOpenFontSettings() },
                    onClick = onOpenFontSettings,
                    showToggle = false,
                )
            }
        }

        // Screen & Lockscreen Timeout Container
        item {
            RoundedCardContainer(title = "Màn hình & Thời gian sáng") {
                SettingsItem(
                    title = "Thời gian sáng màn hình chính",
                    description = "Thời gian duy trì sáng khi đang mở khóa hoặc luôn sáng",
                    icon = Icons.Rounded.Timer,
                    valueText = screenTimeoutSettings.screenTimeoutLabel,
                    onClick = { showScreenTimeoutPicker.value = true },
                )
                SettingsItem(
                    title = "Thời gian sáng màn hình khóa",
                    description = "Thời gian màn hình khóa duy trì sáng trước khi tắt",
                    icon = Icons.Rounded.Lock,
                    valueText = screenTimeoutSettings.lockscreenTimeoutLabel,
                    onClick = { showLockscreenTimeoutPicker.value = true },
                )
            }
        }

        // System Settings & Permissions Container
        item {
            RoundedCardContainer(title = "Hệ thống & Quyền hạn") {
                SettingsItem(
                    title = "Hình nền động Live Wallpaper",
                    description = "Mở trình chọn Live Wallpaper của Android",
                    icon = Icons.Rounded.Image,
                    valueText = if (wallpaperActive) "Đang chạy" else "Cài đặt",
                    onClick = { openWallpaperChooser(context) },
                )
                SettingsItem(
                    title = "Định vị GPS chính xác",
                    description = "Cần thiết để đo tốc độ km/h và dẫn đường thời gian thực",
                    icon = Icons.Rounded.LocationOn,
                    valueText = if (hasLocationPermission) "Đã cấp" else "Chưa cấp",
                    showChevron = false,
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showLockscreenTimeoutPicker.value) {
        TimeoutPresetPickerSheet(
            title = "Thời gian sáng màn hình khóa",
            selectedIndex = screenTimeoutSettings.lockscreenPresetIndex,
            onSelectIndex = { index ->
                scope.launch {
                    screenTimeoutRepository.update { it.copy(lockscreenPresetIndex = index) }
                }
            },
            onDismiss = { showLockscreenTimeoutPicker.value = false },
        )
    }

    if (showScreenTimeoutPicker.value) {
        TimeoutPresetPickerSheet(
            title = "Thời gian sáng màn hình chính",
            selectedIndex = screenTimeoutSettings.screenPresetIndex,
            onSelectIndex = { index ->
                scope.launch {
                    screenTimeoutRepository.update { it.copy(screenPresetIndex = index) }
                }
            },
            onDismiss = { showScreenTimeoutPicker.value = false },
        )
    }
}

private fun openWallpaperChooser(context: Context) {
    val intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, LunarWallpaperService::class.java),
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback =
            Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            context.startActivity(fallback)
        } catch (_: Exception) {
        }
    }
}
