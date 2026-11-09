package com.hongducdev.toolkit.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ToggleOff
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.domain.diy.Automation
import com.hongducdev.toolkit.domain.diy.DIYRepository
import com.hongducdev.toolkit.services.automation.executors.CombinedActionExecutor
import com.hongducdev.toolkit.ui.components.PullTitleHeader
import com.hongducdev.toolkit.ui.components.rememberPullTitleHeaderState
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.ui.diy.AutomationEditorActivity
import com.hongducdev.toolkit.ui.diy.AutomationItem
import com.hongducdev.toolkit.ui.diy.sheets.NewAutomationSheet
import com.hongducdev.toolkit.ui.modifiers.scrollMotionBlur
import com.hongducdev.toolkit.utils.HapticUtil
import kotlinx.coroutines.launch

@Composable
fun DIYScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    newSheetRequested: Boolean = false,
    onNewSheetHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val automations by DIYRepository.automations.collectAsState()

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    val headerState = rememberPullTitleHeaderState()
    val lazyListState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier =
                Modifier
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
                    title = "Tự động hóa",
                    caption = "DIY Automation Rules",
                    state = headerState,
                )
            }

            // Thanh thao tác chọn nhiều (Batch Selection Action Bar)
            if (selectionMode) {
                item {
                    RoundedCardContainer {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Đã chọn: ${selectedIds.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )

                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    selectedIds.forEach { id ->
                                        DIYRepository.getAutomation(id)?.let {
                                            DIYRepository.updateAutomation(it.copy(isEnabled = true))
                                        }
                                    }
                                    exitSelection()
                                },
                            ) {
                                Icon(Icons.Rounded.ToggleOn, contentDescription = "Bật tất cả")
                            }

                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    selectedIds.forEach { id ->
                                        DIYRepository.getAutomation(id)?.let {
                                            DIYRepository.updateAutomation(it.copy(isEnabled = false))
                                        }
                                    }
                                    exitSelection()
                                },
                            ) {
                                Icon(Icons.Rounded.ToggleOff, contentDescription = "Tắt tất cả")
                            }

                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    selectedIds.forEach { DIYRepository.removeAutomation(it) }
                                    exitSelection()
                                },
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Xóa đã chọn",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }

                            IconButton(onClick = { exitSelection() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Đóng")
                            }
                        }
                    }
                }
            }

            if (automations.isEmpty()) {
                item {
                    RoundedCardContainer {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chưa có quy tắc tự động nào",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tạo các kịch bản kích hoạt sự kiện, kiểm soát trạng thái pin, sạc, kết nối hoặc ứng dụng theo ý muốn của bạn.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(automations, key = { it.id }) { rule ->
                    AutomationItem(
                        automation = rule,
                        selectionMode = selectionMode,
                        selected = selectedIds.contains(rule.id),
                        onSelectedChange = { isSelected ->
                            selectionMode = true
                            selectedIds =
                                if (isSelected) {
                                    selectedIds + rule.id
                                } else {
                                    selectedIds - rule.id
                                }
                            if (selectedIds.isEmpty()) exitSelection()
                        },
                        onClick = {
                            val intent =
                                Intent(context, AutomationEditorActivity::class.java).apply {
                                    putExtra(AutomationEditorActivity.EXTRA_AUTOMATION_ID, rule.id)
                                }
                            context.startActivity(intent)
                        },
                        onToggle = {
                            DIYRepository.updateAutomation(rule.copy(isEnabled = !rule.isEnabled))
                        },
                        onDelete = {
                            DIYRepository.removeAutomation(rule.id)
                            Toast.makeText(context, "Đã xóa quy tắc", Toast.LENGTH_SHORT).show()
                        },
                        onTest = {
                            scope.launch {
                                val testActions =
                                    if (rule.type == Automation.Type.TRIGGER) rule.actionList else rule.entryActionList
                                CombinedActionExecutor.executeAll(context, testActions)
                                Toast.makeText(context, "Đã chạy thử: ${rule.name}", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            }
        }
    }

    if (newSheetRequested) {
        NewAutomationSheet(
            onDismiss = onNewSheetHandled,
            onOptionSelected = { type ->
                onNewSheetHandled()
                val intent =
                    Intent(context, AutomationEditorActivity::class.java).apply {
                        putExtra(AutomationEditorActivity.EXTRA_INITIAL_TYPE, type.name)
                    }
                context.startActivity(intent)
            },
        )
    }
}
