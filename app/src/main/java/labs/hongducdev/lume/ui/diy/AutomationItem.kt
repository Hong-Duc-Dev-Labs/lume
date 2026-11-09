package labs.hongducdev.lume.ui.diy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.domain.diy.Automation
import labs.hongducdev.lume.ui.components.menus.SegmentedDropdownMenu
import labs.hongducdev.lume.ui.components.menus.SegmentedDropdownMenuItem
import labs.hongducdev.lume.utils.HapticUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutomationItem(
    automation: Automation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggle: () -> Unit = {},
    onTest: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectedChange: (Boolean) -> Unit = {},
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
            ),
        shape = RoundedCornerShape(18.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (selectionMode) onSelectedChange(!selected) else onClick()
                    },
                    onLongClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (selectionMode) onSelectedChange(!selected) else showMenu = true
                    },
                ).alpha(if (automation.isEnabled) 1f else 0.55f),
    ) {
        Box {
            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = DpOffset(16.dp, 0.dp),
            ) {
                SegmentedDropdownMenuItem(
                    text = { Text("Chạy thử ngay (Test)") },
                    onClick = {
                        showMenu = false
                        onTest()
                    },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                )
                SegmentedDropdownMenuItem(
                    text = { Text("Chỉnh sửa (Edit)") },
                    onClick = {
                        showMenu = false
                        onClick()
                    },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                )
                SegmentedDropdownMenuItem(
                    text = { Text("Chọn mục này") },
                    onClick = {
                        showMenu = false
                        onSelectedChange(true)
                    },
                    leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                )
                SegmentedDropdownMenuItem(
                    text = { Text("Xóa quy tắc") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = selectionMode,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onSelectedChange(it)
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }

                val icon: ImageVector =
                    when (automation.type) {
                        Automation.Type.TRIGGER -> automation.trigger?.icon ?: Icons.Rounded.Bolt
                        Automation.Type.STATE -> automation.state?.icon ?: Icons.Rounded.Timeline
                        Automation.Type.APP -> Icons.Rounded.Apps
                        else -> Icons.Rounded.Bolt
                    }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val title =
                        automation.name.ifBlank {
                            when (automation.type) {
                                Automation.Type.TRIGGER -> automation.trigger?.title ?: "Quy tắc sự kiện"
                                Automation.Type.STATE -> automation.state?.title ?: "Quy tắc trạng thái"
                                Automation.Type.APP -> "Ứng dụng (${automation.selectedApps.size})"
                                else -> "Quy tắc tự động"
                            }
                        }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val actionSummary =
                        if (automation.type == Automation.Type.TRIGGER) {
                            if (automation.actionList.isNotEmpty()) {
                                automation.actionList.joinToString(" • ") { it.title }
                            } else {
                                "Chưa có hành động"
                            }
                        } else {
                            val inCount = automation.entryActionList.size
                            val outCount = automation.exitActionList.size
                            "$inCount hành động khi vào • $outCount khi thoát"
                        }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = actionSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = automation.isEnabled,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onToggle()
                    },
                )
            }
        }
    }
}
