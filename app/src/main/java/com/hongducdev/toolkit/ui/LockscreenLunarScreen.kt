package com.hongducdev.toolkit.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.data.DisplayMode
import com.hongducdev.toolkit.data.LunarDisplayPolicy
import com.hongducdev.toolkit.data.LunarSettings
import com.hongducdev.toolkit.data.LunarSettingsRepository
import com.hongducdev.toolkit.ui.components.LunarMonthCalendar
import com.hongducdev.toolkit.ui.components.PullTitleHeader
import com.hongducdev.toolkit.ui.components.rememberPullTitleHeaderState
import com.hongducdev.toolkit.ui.core.cards.HeroBannerCard
import com.hongducdev.toolkit.ui.core.cards.SettingsItem
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.ui.core.pickers.ColorPickerRow
import com.hongducdev.toolkit.ui.core.pickers.SegmentedPicker
import com.hongducdev.toolkit.ui.core.sliders.ExpressiveSlider
import com.hongducdev.toolkit.ui.modifiers.scrollMotionBlur
import com.hongducdev.toolkit.ui.theme.EssentialsColorTokens
import com.hongducdev.toolkit.ui.theme.ExpressiveRadius
import com.hongducdev.toolkit.wallpaper.LunarWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val textColors =
    listOf(
        0xFFFFFFFF.toInt(),
        0xFFFFE082.toInt(),
        0xFF81D4FA.toInt(),
        0xFFFFAB91.toInt(),
        0xFFA5D6A7.toInt(),
        0xFFE1BEE7.toInt(),
    )

private val modeOptions =
    listOf(
        DisplayMode.AUTO to "Tự động",
        DisplayMode.WALLPAPER_ONLY to "Hình nền",
        DisplayMode.DATE_LINE_ONLY to "Dòng ngày",
    )

enum class LunarViewTab {
    CALENDAR,
    LOCKSCREEN,
}

private val lunarViewTabs =
    listOf(
        LunarViewTab.CALENDAR to "Lịch cả tháng",
        LunarViewTab.LOCKSCREEN to "Màn hình khóa",
    )

/**
 * Chức năng "Lịch âm màn hình khóa".
 * Thiết kế theo chuẩn Material 3 Expressive & sameerasw/essentials.
 */
@Composable
fun LockscreenLunarScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val repository = remember { LunarSettingsRepository(context.applicationContext) }
    val settings by repository.settings.collectAsState(initial = LunarSettings())
    val scope = rememberCoroutineScope()

    val now = System.currentTimeMillis()
    val hookActive =
        LunarDisplayPolicy.isHookActive(
            lastSeenMillis = settings.hookLastSeen,
            bootMillis = now - SystemClock.elapsedRealtime(),
            nowMillis = now,
        )
    val wallpaperDrawing = LunarDisplayPolicy.wallpaperShouldShow(settings.displayMode, hookActive)

    val imagePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { copyBackground(context, uri, repository) }
                    if (saved) {
                        repository.update { it.copy(backgroundVersion = System.currentTimeMillis()) }
                    }
                }
            }
        }

    val headerState = rememberPullTitleHeaderState()
    val lazyListState = rememberLazyListState()
    val hasCustomBg = remember(settings.backgroundVersion) { repository.backgroundFile().exists() }
    var currentTab by rememberSaveable { mutableStateOf(LunarViewTab.CALENDAR) }

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
                title = "Lịch âm",
                caption =
                    if (currentTab == LunarViewTab.CALENDAR) {
                        "Xem lịch âm Việt Nam & tra cứu ngày tốt"
                    } else {
                        "Tùy biến hiển thị âm lịch màn hình khóa"
                    },
                state = headerState,
            )
        }

        // Bộ chuyển đổi giữa "Lịch cả tháng" và "Màn hình khóa"
        item {
            SegmentedPicker(
                items = lunarViewTabs,
                selectedItem = currentTab,
                onItemSelected = { currentTab = it },
            )
        }

        if (currentTab == LunarViewTab.CALENDAR) {
            // Phần xem lịch âm cả tháng
            item {
                LunarMonthCalendar(
                    onNavigateToLockscreen = { currentTab = LunarViewTab.LOCKSCREEN },
                )
            }
        } else {
            // Hero Header Card cho cấu hình màn hình khóa
            item {
                HeroBannerCard(
                    title = "Lịch âm màn hình khóa",
                    description = "Tự động hiển thị âm lịch Việt Nam chính xác (UTC+7) trên màn hình khóa. Tương thích cả khi có Root (Vector Hook) hoặc không Root (Live Wallpaper).",
                    icon = Icons.Rounded.CalendarMonth,
                    statusText =
                        when {
                            hookActive -> "Vector Hook: Hoạt động"
                            wallpaperDrawing -> "Hình nền: Đang vẽ âm lịch"
                            else -> "Đang tắt"
                        },
                    statusColor =
                        if (hookActive ||
                            wallpaperDrawing
                        ) {
                            EssentialsColorTokens.VibrantGreen
                        } else {
                            EssentialsColorTokens.VibrantOrange
                        },
                    pastelColor = EssentialsColorTokens.PastelBlue,
                    vibrantColor = EssentialsColorTokens.VibrantBlue,
                )
            }

            // Display Mode Picker Container
            item {
                RoundedCardContainer(title = "Chế độ hiển thị") {
                    SegmentedPicker(
                        items = modeOptions,
                        selectedItem = settings.displayMode,
                        onItemSelected = { mode ->
                            scope.launch { repository.update { it.copy(displayMode = mode) } }
                        },
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text =
                            when (settings.displayMode) {
                                DisplayMode.AUTO -> "Tự động: dùng dòng ngày hệ thống khi có root & Vector đang chạy, nếu không sẽ vẽ lên hình nền khóa."
                                DisplayMode.WALLPAPER_ONLY -> "Chỉ hình nền: luôn vẽ lịch âm lên hình nền màn hình khóa (không can thiệp hệ thống)."
                                DisplayMode.DATE_LINE_ONLY -> "Chỉ dòng ngày: chỉ hiển thị ghép vào dòng ngày hệ thống (bắt buộc phải có Root & Vector)."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            // System Date Line Configuration (Root Mode)
            item {
                RoundedCardContainer(title = "Dòng ngày hệ thống (Cần Root & Vector)") {
                    SettingsItem(
                        title = "Trạng thái Hook",
                        description = if (hookActive) "Đã kết nối với module System UI Vector" else "Chưa phát hiện module hook Toolkit",
                        icon = Icons.Rounded.Layers,
                        valueText = if (hookActive) "Hoạt động" else "Chưa kích hoạt",
                        showChevron = false,
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Định dạng dòng ngày hiển thị dạng: <ngày hệ thống> · <mẫu của bạn>. Thay đổi có hiệu lực sau tối đa 30 giây.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    Spacer(Modifier.height(8.dp))
                    TemplateEditor(
                        label = "Mẫu cho dòng ngày (nên ngắn gọn)",
                        savedTemplate = settings.hookTemplate,
                        onTemplateChange = { value ->
                            scope.launch { repository.update { it.copy(hookTemplate = value) } }
                        },
                    )
                }
            }

            // Live Wallpaper Customization (Non-root Mode)
            item {
                RoundedCardContainer(title = "Tùy biến hình nền động") {
                    SettingsItem(
                        title = "Cài đặt làm hình nền",
                        description = "Mở trình chọn Live Wallpaper của hệ thống",
                        icon = Icons.Rounded.Wallpaper,
                        valueText = "Mở",
                        onClick = {
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
                        },
                    )

                    Spacer(Modifier.height(8.dp))
                    SettingsItem(
                        title = "Chọn ảnh nền từ máy",
                        description = if (hasCustomBg) "Đang dùng ảnh tự chọn" else "Mặc định (màu đen tối giản)",
                        icon = Icons.Rounded.Image,
                        valueText = "Chọn ảnh",
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )

                    if (hasCustomBg) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { repository.backgroundFile().delete() }
                                    repository.update { it.copy(backgroundVersion = System.currentTimeMillis()) }
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(ExpressiveRadius.Item),
                        ) {
                            Text("Xóa ảnh nền đã chọn (về mặc định)")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    ExpressiveSlider(
                        label = "Tọa độ dọc (Y)",
                        value = settings.yFraction,
                        range = 0.05f..0.95f,
                        valueFormatted = "${(settings.yFraction * 100).toInt()}%",
                        onValueChange = { y -> scope.launch { repository.update { it.copy(yFraction = y) } } },
                    )

                    Spacer(Modifier.height(8.dp))
                    ExpressiveSlider(
                        label = "Kích thước chữ",
                        value = settings.textSizeSp,
                        range = 12f..36f,
                        valueFormatted = "${settings.textSizeSp.toInt()} sp",
                        onValueChange = { size -> scope.launch { repository.update { it.copy(textSizeSp = size) } } },
                    )

                    Spacer(Modifier.height(8.dp))
                    ColorPickerRow(
                        colors = textColors,
                        selectedColorArgb = settings.colorArgb,
                        onColorSelected = { color ->
                            scope.launch { repository.update { it.copy(colorArgb = color) } }
                        },
                    )

                    Spacer(Modifier.height(8.dp))
                    TemplateEditor(
                        label = "Mẫu nội dung hiển thị trên hình nền",
                        savedTemplate = settings.template,
                        onTemplateChange = { value ->
                            scope.launch { repository.update { it.copy(template = value) } }
                        },
                    )
                }
            }

            // Nút quay lại xem lịch âm cả tháng
            item {
                Surface(
                    onClick = { currentTab = LunarViewTab.CALENDAR },
                    shape = RoundedCornerShape(ExpressiveRadius.Card),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .background(EssentialsColorTokens.PastelOrange, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = EssentialsColorTokens.VibrantOrange,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Xem lịch âm cả tháng",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Tra cứu ngày âm, can chi, giờ hoàng đạo & ngày lễ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun copyBackground(
    context: Context,
    uri: Uri,
    repository: LunarSettingsRepository,
): Boolean =
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val target = repository.backgroundFile()
            target.outputStream().use { output ->
                input.copyTo(output)
            }
            true
        } ?: false
    } catch (_: Exception) {
        false
    }
