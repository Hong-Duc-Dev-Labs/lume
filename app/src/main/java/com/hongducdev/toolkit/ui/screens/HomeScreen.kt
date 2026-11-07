package com.hongducdev.toolkit.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hongducdev.toolkit.data.DisplayMode
import com.hongducdev.toolkit.data.LunarDisplayPolicy
import com.hongducdev.toolkit.data.LunarSettings
import com.hongducdev.toolkit.data.LunarSettingsRepository
import com.hongducdev.toolkit.ui.components.DeviceHeroCard
import com.hongducdev.toolkit.ui.components.FavoriteCarousel
import com.hongducdev.toolkit.ui.components.FavoriteItem
import com.hongducdev.toolkit.ui.components.PullTitleHeader
import com.hongducdev.toolkit.ui.components.rememberPullTitleHeaderState
import com.hongducdev.toolkit.ui.modifiers.scrollMotionBlur
import com.hongducdev.toolkit.ui.core.cards.FeatureCard
import com.hongducdev.toolkit.ui.core.cards.SettingsItem
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.wallpaper.LunarWallpaperService
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOpenLunar: () -> Unit,
    onOpenCarDashboard: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { LunarSettingsRepository(context.applicationContext) }
    val settings by repository.settings.collectAsState(initial = LunarSettings())

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

    val hasNotificationPermission = isNotificationServiceEnabled(context)

    val headerState = rememberPullTitleHeaderState()
    val lazyListState = rememberLazyListState()

    val favoriteItems =
        remember(onOpenLunar, onOpenCarDashboard, wallpaperActive) {
            listOf(
                FavoriteItem(
                    id = "lunar",
                    title = "Lịch âm",
                    icon = Icons.Rounded.CalendarMonth,
                    onClick = onOpenLunar,
                ),
                FavoriteItem(
                    id = "car",
                    title = "Car Dashboard",
                    icon = Icons.Rounded.DirectionsCar,
                    onClick = onOpenCarDashboard,
                ),
                FavoriteItem(
                    id = "wallpaper",
                    title = "Live Wallpaper",
                    icon = Icons.Rounded.Image,
                    onClick = { openWallpaperChooser(context) },
                ),
                FavoriteItem(
                    id = "gps",
                    title = "Định vị HUD",
                    icon = Icons.Rounded.LocationOn,
                    onClick = onOpenCarDashboard,
                ),
            )
        }

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
                title = "Essentials",
                caption = "Galaxy S10 5G • Android 16 (AOSP)",
                state = headerState,
            )
        }

        // Device Specs Hero Card (Galaxy S10 5G / Beyondx, Android 16, RAM/Storage)
        item {
            DeviceHeroCard()
        }

        // Horizontal Multi-Browse Favorites Carousel
        item {
            FavoriteCarousel(items = favoriteItems)
        }

        // Main Features Container
        item {
            RoundedCardContainer(title = "Tính năng chính") {
                FeatureCard(
                    title = "Lịch âm màn hình khóa",
                    description = if (hookActive) "Root Hook: Vẽ trực tiếp lên SystemUI" else "Tự động hiển thị trên hình nền Lockscreen",
                    icon = Icons.Rounded.CalendarMonth,
                    isEnabled = settings.displayMode != DisplayMode.DATE_LINE_ONLY,
                    onToggle = { enabled ->
                        scope.launch {
                            repository.update {
                                it.copy(displayMode = if (enabled) DisplayMode.AUTO else DisplayMode.DATE_LINE_ONLY)
                            }
                        }
                    },
                    onClick = onOpenLunar,
                )
                FeatureCard(
                    title = "Chế độ lái xe (Car Dashboard)",
                    description = "Bản đồ MapLibre Native, HUD đo tốc độ GPS & điều khiển Media",
                    icon = Icons.Rounded.DirectionsCar,
                    isEnabled = hasLocationPermission,
                    onToggle = { onOpenCarDashboard() },
                    onClick = onOpenCarDashboard,
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
                    title = "Truy cập thông báo nghe nhạc",
                    description = "Cần thiết để điều khiển MediaSession trong Car Dashboard",
                    icon = Icons.Rounded.Notifications,
                    valueText = if (hasNotificationPermission) "Đã cấp" else "Chưa cấp",
                    onClick = {
                        val intent =
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        context.startActivity(intent)
                    },
                )
                SettingsItem(
                    title = "Định vị GPS chính xác",
                    description = "Theo dõi vị trí xe và đo tốc độ km/h thời gian thực",
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
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat =
        Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
    return flat.split(":").any {
        val cn = ComponentName.unflattenFromString(it)
        cn != null && cn.packageName == pkgName
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
