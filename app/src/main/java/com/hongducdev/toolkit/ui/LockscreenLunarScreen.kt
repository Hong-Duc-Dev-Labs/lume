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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.data.DisplayMode
import com.hongducdev.toolkit.data.LunarDisplayPolicy
import com.hongducdev.toolkit.data.LunarSettings
import com.hongducdev.toolkit.data.LunarSettingsRepository
import com.hongducdev.toolkit.wallpaper.LunarWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val textColors = listOf(
    0xFFFFFFFF.toInt(),
    0xFFFFE082.toInt(),
    0xFF81D4FA.toInt(),
    0xFFFFAB91.toInt(),
    0xFFA5D6A7.toInt(),
)

private val modeLabels = listOf(
    DisplayMode.AUTO to "Tự động",
    DisplayMode.WALLPAPER_ONLY to "Chỉ hình nền",
    DisplayMode.DATE_LINE_ONLY to "Chỉ dòng ngày",
)

/**
 * Chức năng "Lịch âm màn hình khóa". Có root và module hook hoạt động thì lịch âm hiện ngay trong
 * dòng ngày của hệ thống; không thì tự vẽ lên hình nền màn hình khóa.
 */
@Composable
fun LockscreenLunarScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { LunarSettingsRepository(context.applicationContext) }
    val settings by repository.settings.collectAsState(initial = LunarSettings())
    val scope = rememberCoroutineScope()

    val now = System.currentTimeMillis()
    val hookActive = LunarDisplayPolicy.isHookActive(
        lastSeenMillis = settings.hookLastSeen,
        bootMillis = now - SystemClock.elapsedRealtime(),
        nowMillis = now,
    )
    val wallpaperDrawing = LunarDisplayPolicy.wallpaperShouldShow(settings.displayMode, hookActive)

    val imagePicker = rememberLauncherForActivityResult(
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

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Lịch âm màn hình khóa", style = MaterialTheme.typography.headlineSmall)

        StatusCard(hookActive = hookActive, wallpaperDrawing = wallpaperDrawing)

        Text("Cách hiển thị", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modeLabels.forEach { (mode, label) ->
                FilterChip(
                    selected = settings.displayMode == mode,
                    onClick = { scope.launch { repository.update { it.copy(displayMode = mode) } } },
                    label = { Text(label) },
                )
            }
        }
        Text(
            "Tự động: dùng dòng ngày của hệ thống khi máy có root và module Vector đang chạy, " +
                "nếu không thì vẽ lên hình nền.",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(4.dp))
        Text("Dòng ngày của hệ thống (cần root)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Cần cài Vector, bật module Toolkit và chọn phạm vi System UI. " +
                "Thay đổi có hiệu lực sau tối đa 30 giây. Dòng ngày sẽ có dạng: <ngày hệ thống> · <mẫu của bạn>.",
            style = MaterialTheme.typography.bodySmall,
        )
        TemplateEditor(
            label = "Mẫu cho dòng ngày (nên ngắn)",
            savedTemplate = settings.hookTemplate,
            onTemplateChange = { value -> scope.launch { repository.update { it.copy(hookTemplate = value) } } },
        )

        Spacer(Modifier.height(4.dp))
        Text("Hình nền màn hình khóa (không cần root)", style = MaterialTheme.typography.titleMedium)
        TemplateEditor(
            label = "Mẫu hiển thị trên hình nền",
            savedTemplate = settings.template,
            onTemplateChange = { value -> scope.launch { repository.update { it.copy(template = value) } } },
        )

        SliderRow("Cỡ chữ: ${settings.textSizeSp.toInt()} sp", settings.textSizeSp, 12f..72f) { value ->
            scope.launch { repository.update { it.copy(textSizeSp = value) } }
        }
        SliderRow("Vị trí ngang: ${(settings.xFraction * 100).toInt()}%", settings.xFraction, 0f..1f) { value ->
            scope.launch { repository.update { it.copy(xFraction = value) } }
        }
        SliderRow("Vị trí dọc: ${(settings.yFraction * 100).toInt()}%", settings.yFraction, 0f..1f) { value ->
            scope.launch { repository.update { it.copy(yFraction = value) } }
        }

        Text("Màu chữ", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            textColors.forEach { argb ->
                val selected = argb == settings.colorArgb
                Spacer(
                    Modifier
                        .size(36.dp)
                        .background(Color(argb), CircleShape)
                        .border(
                            BorderStroke(if (selected) 3.dp else 1.dp, MaterialTheme.colorScheme.outline),
                            CircleShape,
                        )
                        .clickable { scope.launch { repository.update { it.copy(colorArgb = argb) } } },
                )
            }
        }

        Text("Ảnh nền màn hình khóa", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Chọn ảnh") }
            OutlinedButton(
                enabled = settings.backgroundVersion != 0L,
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.backgroundFile().delete() }
                        repository.update { it.copy(backgroundVersion = 0L) }
                    }
                },
            ) { Text("Dùng nền đen") }
        }

        Button(modifier = Modifier.fillMaxWidth(), onClick = { openWallpaperChooser(context) }) {
            Text("Đặt làm hình nền màn hình khóa")
        }
        Text(
            "Trong màn hình chọn của hệ thống, hãy chọn đặt cho Màn hình khóa. " + wallpaperStatus(context),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StatusCard(hookActive: Boolean, wallpaperDrawing: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Trạng thái", style = MaterialTheme.typography.labelMedium)
            Text(
                if (hookActive) "Module hook đang hoạt động (đã phát hiện root và Vector)."
                else "Chưa phát hiện module hook (không root hoặc chưa bật module).",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                if (wallpaperDrawing) "Hình nền đang vẽ lịch âm." else "Hình nền không vẽ lịch âm.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range)
    }
}

private fun copyBackground(context: Context, uri: Uri, repository: LunarSettingsRepository): Boolean =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            repository.backgroundFile().outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
    }.getOrDefault(false)

private fun openWallpaperChooser(context: Context) {
    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
        putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, LunarWallpaperService::class.java),
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun wallpaperStatus(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return ""
    val manager = WallpaperManager.getInstance(context)
    val lockInfo = manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)
    val active = lockInfo?.component?.className == LunarWallpaperService::class.java.name
    return if (active) "Đang dùng cho màn hình khóa." else "Chưa dùng cho màn hình khóa."
}
