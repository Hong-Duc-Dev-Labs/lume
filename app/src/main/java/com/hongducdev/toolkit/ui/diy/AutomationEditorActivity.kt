package com.hongducdev.toolkit.ui.diy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.domain.diy.Action
import com.hongducdev.toolkit.domain.diy.Automation
import com.hongducdev.toolkit.domain.diy.DIYRepository
import com.hongducdev.toolkit.domain.diy.DiyState
import com.hongducdev.toolkit.domain.diy.Trigger
import com.hongducdev.toolkit.services.automation.executors.CombinedActionExecutor
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.ui.diy.sheets.AppSelectionSheet
import com.hongducdev.toolkit.ui.theme.ToolkitTheme
import com.hongducdev.toolkit.utils.HapticUtil
import kotlinx.coroutines.launch
import java.util.UUID

class AutomationEditorActivity : ComponentActivity() {
    companion object {
        const val EXTRA_AUTOMATION_ID = "extra_automation_id"
        const val EXTRA_INITIAL_TYPE = "extra_initial_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        DIYRepository.init(applicationContext)

        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID)
        val initialTypeStr = intent.getStringExtra(EXTRA_INITIAL_TYPE)
        val initialType =
            try {
                if (initialTypeStr != null) Automation.Type.valueOf(initialTypeStr) else Automation.Type.TRIGGER
            } catch (_: Exception) {
                Automation.Type.TRIGGER
            }

        setContent {
            ToolkitTheme(
                darkTheme = isSystemInDarkTheme(),
                pitchBlackTheme = true,
                dynamicColor = true,
            ) {
                AutomationEditorScreen(
                    automationId = automationId,
                    initialType = initialType,
                    onFinish = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutomationEditorScreen(
    automationId: String?,
    initialType: Automation.Type,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val existing = remember(automationId) { automationId?.let { DIYRepository.getAutomation(it) } }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: initialType) }
    var selectedTrigger by remember { mutableStateOf<Trigger>(existing?.trigger ?: Trigger.ChargerConnected) }
    var selectedState by remember { mutableStateOf<DiyState>(existing?.state ?: DiyState.Charging) }
    var selectedApps by remember { mutableStateOf(existing?.selectedApps ?: emptyList()) }

    var actions by remember { mutableStateOf(existing?.actionList ?: emptyList()) }
    var entryActions by remember { mutableStateOf(existing?.entryActionList ?: emptyList()) }
    var exitActions by remember { mutableStateOf(existing?.exitActionList ?: emptyList()) }

    var showAppPicker by remember { mutableStateOf(false) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existing != null) "Chỉnh sửa quy tắc" else "Tạo quy tắc mới",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val finalName =
                                name.ifBlank {
                                    when (type) {
                                        Automation.Type.TRIGGER -> selectedTrigger.title
                                        Automation.Type.STATE -> selectedState.title
                                        Automation.Type.APP -> "Ứng dụng (${selectedApps.size})"
                                        else -> "Quy tắc tự động"
                                    }
                                }

                            val rule =
                                Automation(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    name = finalName,
                                    type = type,
                                    trigger = if (type == Automation.Type.TRIGGER) selectedTrigger else null,
                                    state = if (type == Automation.Type.STATE) selectedState else null,
                                    actions = actions,
                                    entryActions = entryActions,
                                    exitActions = exitActions,
                                    selectedApps = selectedApps,
                                    isEnabled = existing?.isEnabled ?: true,
                                )

                            if (existing != null) {
                                DIYRepository.updateAutomation(rule)
                            } else {
                                DIYRepository.addAutomation(rule)
                            }

                            Toast.makeText(context, "Đã lưu quy tắc", Toast.LENGTH_SHORT).show()
                            onFinish()
                        },
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Lưu")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên quy tắc (Tùy chọn)") },
                    placeholder = {
                        Text(
                            when (type) {
                                Automation.Type.TRIGGER -> selectedTrigger.title
                                Automation.Type.STATE -> selectedState.title
                                Automation.Type.APP -> "Khi mở ứng dụng"
                                else -> "Quy tắc tự động"
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Chọn Trigger / State / Apps
            item {
                when (type) {
                    Automation.Type.TRIGGER -> {
                        RoundedCardContainer(title = "Sự kiện kích hoạt (Trigger)") {
                            val availableTriggers =
                                listOf(
                                    Trigger.ChargerConnected,
                                    Trigger.ChargerDisconnected,
                                    Trigger.ScreenOn,
                                    Trigger.ScreenOff,
                                    Trigger.DeviceUnlock,
                                    Trigger.PowerSavingOn,
                                    Trigger.PowerSavingOff,
                                    Trigger.DndOn,
                                    Trigger.DndOff,
                                    Trigger.AndroidAutoConnected,
                                    Trigger.AndroidAutoDisconnected,
                                    Trigger.Schedule(hour = 7, minute = 0),
                                    Trigger.WifiConnected(),
                                    Trigger.BluetoothConnected(),
                                )

                            availableTriggers.forEach { trig ->
                                val isSelected = trig::class == selectedTrigger::class
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                selectedTrigger = trig
                                            }.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = trig.icon,
                                            contentDescription = null,
                                            tint =
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            modifier = Modifier.padding(8.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = trig.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    Automation.Type.STATE -> {
                        RoundedCardContainer(title = "Điều kiện trạng thái (State)") {
                            val availableStates =
                                listOf(
                                    DiyState.Charging,
                                    DiyState.ScreenOn,
                                    DiyState.PowerSaving,
                                    DiyState.BatteryLevel(minLevel = 0, maxLevel = 20),
                                    DiyState.TimePeriod(startHour = 22, startMinute = 0, endHour = 7, endMinute = 0),
                                )

                            availableStates.forEach { st ->
                                val isSelected = st::class == selectedState::class
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                selectedState = st
                                            }.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = st.icon,
                                            contentDescription = null,
                                            tint =
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            modifier = Modifier.padding(8.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = st.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    Automation.Type.APP -> {
                        RoundedCardContainer(title = "Ứng dụng kích hoạt") {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Đã chọn ${selectedApps.size} ứng dụng",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { showAppPicker = true }) {
                                    Text("Chọn danh sách ứng dụng")
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }

            // Phần thiết lập hành động thực thi (Actions)
            item {
                if (type == Automation.Type.TRIGGER) {
                    RoundedCardContainer(title = "Hành động thực thi (Actions)") {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ActionSequenceEditor(
                                actions = actions,
                                onActionsChange = { actions = it },
                            )
                        }
                    }
                } else {
                    RoundedCardContainer(title = "Khi bắt đầu điều kiện (Entry Actions)") {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ActionSequenceEditor(
                                actions = entryActions,
                                onActionsChange = { entryActions = it },
                                emptyMessage = "Chưa có hành động khi bắt đầu.",
                            )
                        }
                    }

                    RoundedCardContainer(title = "Khi kết thúc điều kiện (Exit Actions)") {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ActionSequenceEditor(
                                actions = exitActions,
                                onActionsChange = { exitActions = it },
                                emptyMessage = "Chưa có hành động khi kết thúc.",
                            )
                        }
                    }
                }
            }

            // Nút chạy thử nghiệm ngay (Test)
            item {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        scope.launch {
                            val testList = if (type == Automation.Type.TRIGGER) actions else entryActions
                            CombinedActionExecutor.executeAll(context, testList)
                            Toast.makeText(context, "Đã chạy thử chuỗi hành động!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chạy thử chuỗi hành động ngay (Test)")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAppPicker) {
        AppSelectionSheet(
            initialSelected = selectedApps,
            multiSelect = true,
            onDismiss = { showAppPicker = false },
            onConfirmed = { apps ->
                selectedApps = apps.map { it.packageName }
            },
        )
    }
}
