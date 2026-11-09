package com.hongducdev.toolkit.ui.qr

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.hongducdev.toolkit.qr.QrParsedData
import com.hongducdev.toolkit.qr.QrResult
import com.hongducdev.toolkit.qr.QrType
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.ui.theme.EssentialsColorTokens
import com.hongducdev.toolkit.ui.theme.ExpressiveRadius
import com.hongducdev.toolkit.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrResultSheet(
    result: QrResult,
    onDismissRequest: () -> Unit,
    onScanAgain: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val (badgeBg, badgeTint, badgeIcon) =
        remember(result.type, isDark) {
            when (result.type) {
                QrType.URL -> {
                    Triple(
                        if (isDark) EssentialsColorTokens.DarkPastelBlue else EssentialsColorTokens.PastelBlue,
                        EssentialsColorTokens.VibrantBlue,
                        Icons.Rounded.Language,
                    )
                }

                QrType.WIFI -> {
                    Triple(
                        if (isDark) EssentialsColorTokens.DarkPastelGreen else EssentialsColorTokens.PastelGreen,
                        EssentialsColorTokens.VibrantGreen,
                        Icons.Rounded.Wifi,
                    )
                }

                QrType.PHONE -> {
                    Triple(
                        if (isDark) EssentialsColorTokens.DarkPastelOrange else EssentialsColorTokens.PastelOrange,
                        EssentialsColorTokens.VibrantOrange,
                        Icons.Rounded.Call,
                    )
                }

                QrType.SMS -> {
                    Triple(
                        if (isDark) EssentialsColorTokens.DarkPastelPurple else EssentialsColorTokens.PastelPurple,
                        EssentialsColorTokens.VibrantPurple,
                        Icons.Rounded.Sms,
                    )
                }

                QrType.EMAIL -> {
                    Triple(
                        if (isDark) EssentialsColorTokens.DarkPastelPurple else EssentialsColorTokens.PastelPurple,
                        EssentialsColorTokens.VibrantPurple,
                        Icons.Rounded.Email,
                    )
                }

                QrType.GEO -> {
                    Triple(
                        if (isDark) EssentialsColorTokens.DarkPastelRose else EssentialsColorTokens.PastelRose,
                        EssentialsColorTokens.VibrantRose,
                        Icons.Rounded.LocationOn,
                    )
                }

                QrType.TEXT -> {
                    Triple(
                        if (isDark) EssentialsColorTokens.DarkPastelBlue else EssentialsColorTokens.PastelBlue,
                        EssentialsColorTokens.VibrantBlue,
                        Icons.AutoMirrored.Rounded.Notes,
                    )
                }
            }
        }

    fun copyToClipboard(
        text: String,
        label: String = "Mã QR",
    ) {
        HapticUtil.performUIHaptic(view)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Đã sao chép vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        HapticUtil.performUIHaptic(view)
        val sendIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
        val shareIntent = Intent.createChooser(sendIntent, "Chia sẻ mã QR")
        context.startActivity(shareIntent)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Loại mã QR + icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(badgeBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        badgeIcon,
                        contentDescription = null,
                        tint = badgeTint,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.type.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Đã quét thành công",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Container nội dung chi tiết
            RoundedCardContainer {
                when (val data = result.data) {
                    is QrParsedData.Url -> {
                        DetailRow(label = "Tên miền", value = data.host.ifBlank { "Website" })
                        DetailRow(label = "Đường dẫn", value = data.url, isSelectable = true)
                    }

                    is QrParsedData.Wifi -> {
                        DetailRow(label = "Tên mạng (SSID)", value = data.ssid)
                        if (data.password.isNotBlank()) {
                            DetailRow(label = "Mật khẩu", value = data.password, isSelectable = true)
                        } else {
                            DetailRow(label = "Mật khẩu", value = "Không có (Mạng mở)")
                        }
                        DetailRow(label = "Chuẩn bảo mật", value = data.security)
                    }

                    is QrParsedData.Phone -> {
                        DetailRow(label = "Số điện thoại", value = data.number, isSelectable = true)
                    }

                    is QrParsedData.Sms -> {
                        DetailRow(label = "Số người nhận", value = data.number)
                        if (data.message.isNotBlank()) {
                            DetailRow(label = "Nội dung tin nhắn", value = data.message, isSelectable = true)
                        }
                    }

                    is QrParsedData.Email -> {
                        DetailRow(label = "Địa chỉ email", value = data.email, isSelectable = true)
                        if (data.subject.isNotBlank()) {
                            DetailRow(label = "Tiêu đề", value = data.subject)
                        }
                        if (data.body.isNotBlank()) {
                            DetailRow(label = "Nội dung", value = data.body, isSelectable = true)
                        }
                    }

                    is QrParsedData.Geo -> {
                        DetailRow(label = "Vĩ độ (Lat)", value = data.latitude.toString())
                        DetailRow(label = "Kinh độ (Lng)", value = data.longitude.toString())
                        if (data.query != null) {
                            DetailRow(label = "Chi tiết", value = data.query)
                        }
                    }

                    is QrParsedData.Text -> {
                        DetailRow(label = "Nội dung văn bản", value = data.content, isSelectable = true)
                    }
                }
            }

            // Primary Action Buttons theo loại mã
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (val data = result.data) {
                    is QrParsedData.Url -> {
                        Button(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Không thể mở trình duyệt", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ExpressiveRadius.Button),
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mở liên kết web", fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { copyToClipboard(data.url, "URL") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(ExpressiveRadius.Button),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sao chép")
                            }
                            OutlinedButton(
                                onClick = { shareText(data.url) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(ExpressiveRadius.Button),
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Chia sẻ")
                            }
                        }
                    }

                    is QrParsedData.Wifi -> {
                        if (data.password.isNotBlank()) {
                            Button(
                                onClick = { copyToClipboard(data.password, "Mật khẩu Wi-Fi") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ExpressiveRadius.Button),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sao chép mật khẩu Wi-Fi", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ExpressiveRadius.Button),
                        ) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mở cài đặt Wi-Fi máy")
                        }
                    }

                    is QrParsedData.Phone -> {
                        Button(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                try {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${data.number}")))
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ExpressiveRadius.Button),
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gọi ngay", fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { copyToClipboard(data.number, "Số điện thoại") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(ExpressiveRadius.Button),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sao chép")
                            }
                            OutlinedButton(
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${data.number}")))
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(ExpressiveRadius.Button),
                            ) {
                                Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Nhắn tin")
                            }
                        }
                    }

                    is QrParsedData.Sms -> {
                        Button(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                try {
                                    val uri = Uri.parse("smsto:${data.number}")
                                    val intent =
                                        Intent(Intent.ACTION_SENDTO, uri).apply {
                                            putExtra("sms_body", data.message)
                                        }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ExpressiveRadius.Button),
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gửi tin nhắn SMS", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    is QrParsedData.Email -> {
                        Button(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                try {
                                    val intent =
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${data.email}")).apply {
                                            putExtra(Intent.EXTRA_SUBJECT, data.subject)
                                            putExtra(Intent.EXTRA_TEXT, data.body)
                                        }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ExpressiveRadius.Button),
                        ) {
                            Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gửi Email", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    is QrParsedData.Geo -> {
                        Button(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                try {
                                    val uri = Uri.parse("geo:${data.latitude},${data.longitude}?q=${data.latitude},${data.longitude}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ExpressiveRadius.Button),
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mở bản đồ dẫn đường", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    is QrParsedData.Text -> {
                        Button(
                            onClick = { copyToClipboard(data.content, "Văn bản") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ExpressiveRadius.Button),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sao chép văn bản", fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    try {
                                        val intent =
                                            Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                putExtra(SearchManager.QUERY, data.content)
                                            }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(ExpressiveRadius.Button),
                            ) {
                                Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Tìm trên Web")
                            }
                            OutlinedButton(
                                onClick = { shareText(data.content) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(ExpressiveRadius.Button),
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Chia sẻ")
                            }
                        }
                    }
                }

                FilledTonalButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onScanAgain()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ExpressiveRadius.Button),
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Quét tiếp mã khác")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isSelectable: Boolean = false,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
