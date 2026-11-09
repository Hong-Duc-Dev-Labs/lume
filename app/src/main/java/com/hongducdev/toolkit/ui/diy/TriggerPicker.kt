package com.hongducdev.toolkit.ui.diy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.domain.diy.Automation
import com.hongducdev.toolkit.domain.diy.DiyState
import com.hongducdev.toolkit.domain.diy.Trigger
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.utils.HapticUtil

private class PickerItem(
    val title: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

private class PickerGroup(
    val title: String,
    val items: List<PickerItem>,
)

/**
 * "Select Trigger" panel ported from sameerasw/essentials AutomationEditorActivity:
 * type chips, collapsible categories with a count badge, and radio rows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TriggerPicker(
    type: Automation.Type,
    onTypeChange: (Automation.Type) -> Unit,
    selectedTrigger: Trigger,
    onTriggerChange: (Trigger) -> Unit,
    selectedState: DiyState,
    onStateChange: (DiyState) -> Unit,
    selectedAppCount: Int,
    onPickApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = if (type == Automation.Type.STATE) "Chọn trạng thái" else "Chọn kích hoạt",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        RoundedCardContainer {
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Automation.Type.TRIGGER to "Sự kiện",
                    Automation.Type.STATE to "Trạng thái",
                    Automation.Type.APP to "Ứng dụng",
                ).forEach { (t, label) ->
                    FilterChip(
                        selected = type == t,
                        onClick = {
                            if (type != t) {
                                HapticUtil.performUIHaptic(view)
                                onTypeChange(t)
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }
        }

        when (type) {
            Automation.Type.TRIGGER -> CategoryList(triggerGroups(selectedTrigger) { onTriggerChange(it) })
            Automation.Type.STATE -> CategoryList(stateGroups(selectedState) { onStateChange(it) })
            Automation.Type.APP ->
                RoundedCardContainer(spacing = 2.dp) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Đã chọn $selectedAppCount ứng dụng",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Button(onClick = onPickApps) { Text("Chọn danh sách ứng dụng") }
                    }
                }

            else -> Unit
        }
    }
}

@Composable
private fun CategoryList(groups: List<PickerGroup>) {
    val view = LocalView.current
    var expanded by remember(groups.size) {
        mutableStateOf(
            (groups.firstOrNull { g -> g.items.any { it.selected } } ?: groups.firstOrNull())?.title,
        )
    }

    groups.forEach { group ->
        val isOpen = expanded == group.title
        val rotation by animateFloatAsState(if (isOpen) 180f else 0f, label = "ChevronRotation")

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            HapticUtil.performUIHaptic(view)
                            expanded = if (isOpen) null else group.title
                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = group.items.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp).rotate(rotation),
                )
            }

            AnimatedVisibility(
                visible = isOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                RoundedCardContainer(spacing = 2.dp) {
                    group.items.forEach { item ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceBright)
                                    .clickable {
                                        HapticUtil.performUIHaptic(view)
                                        item.onSelect()
                                    }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = item.selected, onClick = item.onSelect)
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                color =
                                    if (item.selected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun triggerGroups(
    selected: Trigger,
    onSelect: (Trigger) -> Unit,
): List<PickerGroup> {
    fun item(t: Trigger) = PickerItem(t.title, t.icon, t::class == selected::class) { onSelect(t) }

    return listOf(
        PickerGroup(
            "Hệ thống & Màn hình",
            listOf(Trigger.ScreenOff, Trigger.ScreenOn, Trigger.DeviceUnlock).map(::item),
        ),
        PickerGroup(
            "Pin & Nguồn",
            listOf(
                Trigger.ChargerConnected,
                Trigger.ChargerDisconnected,
                Trigger.PowerSavingOn,
                Trigger.PowerSavingOff,
            ).map(::item),
        ),
        PickerGroup(
            "Kết nối & Mạng",
            listOf(
                Trigger.WifiConnected(),
                Trigger.BluetoothConnected(),
                Trigger.AndroidAutoConnected,
                Trigger.AndroidAutoDisconnected,
            ).map(::item),
        ),
        PickerGroup(
            "Thông báo",
            listOf(Trigger.DndOn, Trigger.DndOff).map(::item),
        ),
        PickerGroup(
            "Thời gian",
            listOf(Trigger.Schedule(hour = 7, minute = 0)).map(::item),
        ),
    )
}

private fun stateGroups(
    selected: DiyState,
    onSelect: (DiyState) -> Unit,
): List<PickerGroup> {
    fun item(s: DiyState) = PickerItem(s.title, s.icon, s::class == selected::class) { onSelect(s) }

    return listOf(
        PickerGroup(
            "Pin & Nguồn",
            listOf(
                DiyState.Charging,
                DiyState.BatteryLevel(minLevel = 0, maxLevel = 20),
                DiyState.PowerSaving,
            ).map(::item),
        ),
        PickerGroup("Hệ thống & Màn hình", listOf(DiyState.ScreenOn).map(::item)),
        PickerGroup(
            "Thời gian",
            listOf(DiyState.TimePeriod(startHour = 22, startMinute = 0, endHour = 7, endMinute = 0)).map(::item),
        ),
    )
}
