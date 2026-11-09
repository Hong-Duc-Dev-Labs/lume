package labs.hongducdev.lume.ui.diy.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.domain.diy.Automation
import labs.hongducdev.lume.ui.core.containers.RoundedCardContainer
import labs.hongducdev.lume.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAutomationSheet(
    onDismiss: () -> Unit,
    onOptionSelected: (Automation.Type) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Tạo tự động hóa mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Chọn cơ chế kích hoạt để bắt đầu tạo kịch bản cho thiết bị của bạn",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoundedCardContainer {
                AutomationTypeOption(
                    title = "Sự kiện (Trigger)",
                    description = "Chạy hành động khi có sự kiện xảy ra (cắm sạc, mở khóa, theo giờ, kết nối Wifi/Bluetooth...)",
                    icon = Icons.Rounded.Bolt,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onOptionSelected(Automation.Type.TRIGGER)
                    },
                )
                AutomationTypeOption(
                    title = "Trạng thái (State)",
                    description = "Thực thi hành động khi bắt đầu và khi kết thúc điều kiện (đang sạc, trong khoảng giờ, mức pin...)",
                    icon = Icons.Rounded.Timeline,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onOptionSelected(Automation.Type.STATE)
                    },
                )
                AutomationTypeOption(
                    title = "Theo ứng dụng (App)",
                    description = "Tự động kích hoạt khi mở hoặc đóng các ứng dụng bạn chọn",
                    icon = Icons.Rounded.Apps,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onOptionSelected(Automation.Type.APP)
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AutomationTypeOption(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
