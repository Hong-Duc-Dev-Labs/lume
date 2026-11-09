package com.hongducdev.toolkit.ui.screens

import android.content.Context
import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hongducdev.toolkit.data.FontSettingsRepository
import com.hongducdev.toolkit.ui.components.PullTitleHeader
import com.hongducdev.toolkit.ui.components.rememberPullTitleHeaderState
import com.hongducdev.toolkit.ui.components.EssentialsFloatingToolbar
import com.hongducdev.toolkit.ui.core.cards.HeroBannerCard
import com.hongducdev.toolkit.ui.core.cards.IconToggleItem
import com.hongducdev.toolkit.ui.core.cards.SettingsItem
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.ui.modifiers.scrollMotionBlur
import com.hongducdev.toolkit.ui.theme.EssentialsColorTokens
import com.hongducdev.toolkit.ui.theme.ExpressiveRadius
import com.hongducdev.toolkit.utils.HapticUtil
import com.hongducdev.toolkit.utils.ShellUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Màn hình cài đặt và xem trước phông chữ giao diện hệ thống (System UI Font).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FontSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val repository = remember { FontSettingsRepository(context) }
    val settings by repository.settings.collectAsState(
        initial =
            com.hongducdev.toolkit.data
                .FontSettings(),
    )

    val headerState = rememberPullTitleHeaderState()
    val lazyListState = rememberLazyListState()

    val fontFile = remember(settings.fontVersion) { repository.getFontFile() }
    val hasFontFile = remember(settings.fontVersion) { fontFile.exists() && fontFile.length() > 0L }

    val previewFontFamily =
        remember(settings.fontVersion, hasFontFile) {
            if (hasFontFile) {
                try {
                    FontFamily(Typeface.createFromFile(fontFile))
                } catch (_: Throwable) {
                    FontFamily.Default
                }
            } else {
                FontFamily.Default
            }
        }

    // Bộ chọn file font (.ttf, .otf) qua SAF
    val fontPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    HapticUtil.performUIHaptic(view)
                    val result = repository.saveFontFromUri(uri)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Đã nạp file font: ${result.getOrNull()?.fontName}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Lỗi khi nạp font: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier =
                modifier
                    .fillMaxSize()
                    .nestedScroll(headerState.nestedScrollConnection)
                    .scrollMotionBlur(lazyListState),
            contentPadding =
                PaddingValues(
                    top = contentPadding.calculateTopPadding() + 56.dp,
                    bottom = contentPadding.calculateBottomPadding() + 80.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero Banner Card
            item {
                HeroBannerCard(
                    title = "Phông chữ System UI",
                    description = "Thay thế phông chữ mặc định của hệ thống (Status bar, Quick Settings, Màn hình khóa) bằng file font .ttf / .otf bạn yêu thích.",
                    icon = Icons.Rounded.TextFields,
                    statusText =
                        when {
                            settings.isEnabled && hasFontFile -> "Đang áp dụng: ${settings.fontName}"
                            hasFontFile -> "Đã nạp file (chưa bật)"
                            else -> "Đang dùng font mặc định"
                        },
                    statusColor =
                        when {
                            settings.isEnabled && hasFontFile -> EssentialsColorTokens.VibrantGreen
                            hasFontFile -> EssentialsColorTokens.VibrantOrange
                            else -> EssentialsColorTokens.VibrantBlue
                        },
                    pastelColor = EssentialsColorTokens.PastelPurple,
                    vibrantColor = EssentialsColorTokens.VibrantPurple,
                )
            }

            // Container: Chọn & Kích hoạt Font
            item {
                RoundedCardContainer(title = "Quản lý phông chữ") {
                    IconToggleItem(
                        title = "Kích hoạt font tùy chỉnh",
                        description =
                            if (hasFontFile) {
                                "Áp dụng font ${settings.fontName} cho giao diện hệ thống"
                            } else {
                                "Hãy chọn một file font (.ttf hoặc .otf) trước khi bật"
                            },
                        icon = Icons.Rounded.CheckCircle,
                        checked = settings.isEnabled && hasFontFile,
                        enabled = hasFontFile,
                        onCheckedChange = { enabled ->
                            HapticUtil.performVirtualKeyHaptic(view)
                            scope.launch {
                                repository.setEnabled(enabled)
                            }
                        },
                    )

                    Spacer(Modifier.height(8.dp))

                    SettingsItem(
                        title = "Chọn file font từ máy",
                        description =
                            if (hasFontFile) {
                                "Đang chọn: ${settings.fontName} (${formatFileSize(settings.fileSize)})"
                            } else {
                                "Hỗ trợ định dạng TrueType (.ttf) và OpenType (.otf)"
                            },
                        icon = Icons.Rounded.FolderOpen,
                        valueText = if (hasFontFile) "Đổi file" else "Chọn file",
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            fontPickerLauncher.launch(
                                arrayOf(
                                    "font/*",
                                    "font/ttf",
                                    "font/otf",
                                    "application/x-font-ttf",
                                    "application/x-font-opentype",
                                    "application/octet-stream",
                                    "*/*",
                                ),
                            )
                        },
                    )

                    if (hasFontFile) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                scope.launch {
                                    repository.resetFont()
                                    Toast.makeText(context, "Đã khôi phục font mặc định của hệ thống", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(ExpressiveRadius.Item),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Khôi phục font hệ thống mặc định")
                        }
                    }
                }
            }

            // Container: Xem trước phông chữ (Font Preview)
            item {
                RoundedCardContainer(title = "Bản xem trước phông chữ") {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ExpressiveRadius.Item))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Mẫu đồng hồ lớn kiểu Lockscreen
                        Column {
                            Text(
                                text = "ĐỒNG HỒ & SỐ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "12:45 CH",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Thứ Hai, 9 tháng 11 · 29/9 Âm lịch",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Mẫu bảng chữ cái Tiếng Việt đầy đủ dấu
                        Column {
                            Text(
                                text = "BẢNG CHỮ CÁI TIẾNG VIỆT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "A Ă Â B C D Đ E Ê G H I K L M N O Ô Ơ P Q R S T U Ư V X Y",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "a ă â b c d đ e ê g h i k l m n o ô ơ p q r s t u ư v x y",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Mẫu câu văn bản giao diện
                        Column {
                            Text(
                                text = "VĂN BẢN GIAO DIỆN HỆ THỐNG",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Cộng hòa Xã hội Chủ nghĩa Việt Nam",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Độc lập - Tự do - Hạnh phúc",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Trăng thanh gió mát bên hồ, lòng người thanh thản nhẹ nhàng như mây bay.",
                                fontSize = 14.sp,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Các kiểu độ đậm nhạt (Font Weight Variations)
                        Column {
                            Text(
                                text = "CÁC KIỂU TRÌNH BÀY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Kiểu chữ thanh mảnh (Light 300)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Kiểu chữ thường chuẩn (Regular 400)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Kiểu chữ trung bình (Medium 500)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Kiểu chữ đậm nổi bật (Bold 700)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Kiểu chữ in nghiêng (Italic)",
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                fontFamily = previewFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Container: Áp dụng & Khởi động lại SystemUI
            item {
                RoundedCardContainer(title = "Áp dụng vào hệ thống") {
                    Text(
                        text = "Sau khi chọn font và bật công tắc, hãy khởi động lại SystemUI (hoặc khởi động lại máy) để toàn bộ giao diện Status Bar, Quick Settings và Màn hình khóa nạp phông chữ mới.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    Spacer(Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = {
                            HapticUtil.performHeavyHaptic(view)
                            scope.launch {
                                Toast.makeText(context, "Đang khởi động lại SystemUI...", Toast.LENGTH_SHORT).show()
                                val success =
                                    withContext(Dispatchers.IO) {
                                        ShellUtil.runRoot("killall com.android.systemui") ||
                                            ShellUtil.runRoot("pkill -f com.android.systemui")
                                    }
                                if (!success) {
                                    Toast
                                        .makeText(
                                            context,
                                            "Không thể khởi động lại tự động (chưa có quyền Root). Vui lòng khởi động lại máy.",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(ExpressiveRadius.Item),
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Khởi động lại SystemUI ngay (Cần Root)")
                    }
                }
            }
        }

        EssentialsFloatingToolbar(
            title = "Font UI hệ thống",
            onBackClick = onBack,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    return if (kb >= 1024.0) {
        String.format("%.2f MB", kb / 1024.0)
    } else {
        String.format("%.1f KB", kb)
    }
}
