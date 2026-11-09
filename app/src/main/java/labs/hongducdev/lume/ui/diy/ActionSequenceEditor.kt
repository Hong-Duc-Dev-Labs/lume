package labs.hongducdev.lume.ui.diy

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.domain.diy.Action
import labs.hongducdev.lume.domain.diy.ActionRegistry
import labs.hongducdev.lume.ui.core.containers.RoundedCardContainer
import labs.hongducdev.lume.ui.diy.sheets.AppSelectionSheet
import labs.hongducdev.lume.ui.diy.sheets.CustomSettingsSheet
import labs.hongducdev.lume.ui.diy.sheets.SendNotificationSettingsSheet
import labs.hongducdev.lume.ui.diy.sheets.SoundModeSettingsSheet
import labs.hongducdev.lume.ui.diy.sheets.VolumeSettingsSheet
import labs.hongducdev.lume.ui.diy.sheets.WaitSettingsSheet
import labs.hongducdev.lume.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSequenceEditor(
    actions: List<Action>,
    onActionsChange: (List<Action>) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Chưa có hành động nào trong chuỗi này.",
) {
    val view = LocalView.current
    var showAddActionSheet by remember { mutableStateOf(false) }

    // State for editing existing action
    var editingIndex by remember { mutableIntStateOf(-1) }
    var actionBeingConfigured by remember { mutableStateOf<Action?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (actions.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            actions.forEachIndexed { index, action ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = action.isConfigurable) {
                                editingIndex = index
                                actionBeingConfigured = action
                            },
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (action.isConfigurable) {
                                Text(
                                    text = "Nhấn để tùy chỉnh thông số",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val updated = actions.toMutableList().apply { removeAt(index) }
                                onActionsChange(updated)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Xóa",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                showAddActionSheet = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Thêm hành động")
        }
    }

    // Modal Sheet chọn Action mới
    if (showAddActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddActionSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Thêm hành động",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val categories = ActionRegistry.getCategories()
                    items(categories) { category ->
                        RoundedCardContainer(title = category.title) {
                            category.actions.forEach { act ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showAddActionSheet = false
                                                val updated = actions + act
                                                onActionsChange(updated)
                                                if (act.isConfigurable) {
                                                    editingIndex = updated.lastIndex
                                                    actionBeingConfigured = act
                                                }
                                            }.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = act.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = act.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheets tùy biến tham số hành động
    when (val currentAction = actionBeingConfigured) {
        is Action.Wait -> {
            WaitSettingsSheet(
                initialAction = currentAction,
                onDismiss = { actionBeingConfigured = null },
                onSave = { updatedAction ->
                    if (editingIndex in actions.indices) {
                        val list = actions.toMutableList()
                        list[editingIndex] = updatedAction
                        onActionsChange(list)
                    }
                },
            )
        }

        is Action.SendNotification -> {
            SendNotificationSettingsSheet(
                initialAction = currentAction,
                onDismiss = { actionBeingConfigured = null },
                onSave = { updatedAction ->
                    if (editingIndex in actions.indices) {
                        val list = actions.toMutableList()
                        list[editingIndex] = updatedAction
                        onActionsChange(list)
                    }
                },
            )
        }

        is Action.SoundMode -> {
            SoundModeSettingsSheet(
                initialAction = currentAction,
                onDismiss = { actionBeingConfigured = null },
                onSave = { updatedAction ->
                    if (editingIndex in actions.indices) {
                        val list = actions.toMutableList()
                        list[editingIndex] = updatedAction
                        onActionsChange(list)
                    }
                },
            )
        }

        is Action.SetVolume -> {
            VolumeSettingsSheet(
                initialAction = currentAction,
                onDismiss = { actionBeingConfigured = null },
                onSave = { updatedAction ->
                    if (editingIndex in actions.indices) {
                        val list = actions.toMutableList()
                        list[editingIndex] = updatedAction
                        onActionsChange(list)
                    }
                },
            )
        }

        is Action.CustomSettings -> {
            CustomSettingsSheet(
                initialAction = currentAction,
                onDismiss = { actionBeingConfigured = null },
                onSave = { updatedAction ->
                    if (editingIndex in actions.indices) {
                        val list = actions.toMutableList()
                        list[editingIndex] = updatedAction
                        onActionsChange(list)
                    }
                },
            )
        }

        is Action.OpenApp -> {
            AppSelectionSheet(
                multiSelect = false,
                onDismiss = { actionBeingConfigured = null },
                onConfirmed = { selectedApps ->
                    val app = selectedApps.firstOrNull() ?: return@AppSelectionSheet
                    if (editingIndex in actions.indices) {
                        val list = actions.toMutableList()
                        list[editingIndex] = Action.OpenApp(packageName = app.packageName, appName = app.appName)
                        onActionsChange(list)
                    }
                },
            )
        }

        else -> {}
    }
}
