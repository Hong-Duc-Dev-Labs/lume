package labs.hongducdev.lume.ui.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.qr.QrHistoryItem
import labs.hongducdev.lume.qr.QrHistoryRepository
import labs.hongducdev.lume.qr.QrParser
import labs.hongducdev.lume.qr.QrResult
import labs.hongducdev.lume.qr.QrType
import labs.hongducdev.lume.ui.core.containers.RoundedCardContainer
import labs.hongducdev.lume.ui.theme.EssentialsColorTokens
import labs.hongducdev.lume.ui.theme.ExpressiveRadius
import labs.hongducdev.lume.utils.HapticUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrHistorySheet(
    onDismissRequest: () -> Unit,
    onSelectItem: (QrResult) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val repository = remember { QrHistoryRepository(context.applicationContext) }
    val historyItems by repository.history.collectAsState(initial = emptyList())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
        ) {
            // Header bar
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Lịch sử quét QR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (historyItems.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            scope.launch { repository.clear() }
                        },
                    ) {
                        Text("Xóa tất cả", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (historyItems.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            text = "Chưa có mã QR nào được quét gần đây",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(historyItems, key = { it.id }) { item ->
                        HistoryRowItem(
                            item = item,
                            dateFormat = dateFormat,
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                val parsed = QrParser.parse(item.rawText)
                                onSelectItem(parsed)
                            },
                            onDelete = {
                                HapticUtil.performUIHaptic(view)
                                scope.launch { repository.delete(item.id) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRowItem(
    item: QrHistoryItem,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val (badgeBg, badgeTint, badgeIcon) =
        remember(item.type, isDark) {
            when (item.type) {
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

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ExpressiveRadius.Card))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(badgeBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    badgeIcon,
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.subtitle} • ${dateFormat.format(Date(item.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Xóa",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
