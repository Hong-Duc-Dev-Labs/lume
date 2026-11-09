package com.hongducdev.toolkit.ui.diy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.hongducdev.toolkit.ui.components.EssentialsFloatingToolbar
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.ui.core.pickers.SegmentedPicker
import com.hongducdev.toolkit.ui.diy.sheets.AppSelectionSheet
import com.hongducdev.toolkit.ui.modifiers.scrollMotionBlur
import com.hongducdev.toolkit.ui.theme.ToolkitTheme
import com.hongducdev.toolkit.utils.HapticUtil
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    var actionTab by remember { mutableIntStateOf(0) }

    var showAppPicker by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    fun goBack() {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
        } else {
            onFinish()
        }
    }

    fun save() {
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

        if (existing != null) DIYRepository.updateAutomation(rule) else DIYRepository.addAutomation(rule)
        Toast.makeText(context, "Đã lưu quy tắc", Toast.LENGTH_SHORT).show()
        onFinish()
    }

    BackHandler { goBack() }

    // Rung nhẹ khi vuốt giữa các trang, rung mạnh khi trang đã chốt, giống Essentials.
    val swipeHaptics =
        remember {
            object : NestedScrollConnection {
                var accumulated = 0f

                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput) {
                        accumulated += available.x
                        if (abs(accumulated) >= 40f) {
                            HapticUtil.performSliderHaptic(view)
                            accumulated = 0f
                        }
                    }
                    return Offset.Zero
                }
            }
        }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { HapticUtil.performHeavyHaptic(view) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().nestedScroll(swipeHaptics),
                contentPadding = PaddingValues(horizontal = 18.dp),
                pageSpacing = 4.dp,
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().scrollMotionBlur(listState),
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = statusBarHeight + 20.dp,
                                bottom = navBarHeight + 120.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (page == 0) {
                            item {
                                TriggerPicker(
                                    type = type,
                                    onTypeChange = { type = it },
                                    selectedTrigger = selectedTrigger,
                                    onTriggerChange = { selectedTrigger = it },
                                    selectedState = selectedState,
                                    onStateChange = { selectedState = it },
                                    selectedAppCount = selectedApps.size,
                                    onPickApps = { showAppPicker = true },
                                )
                            }
                        } else {
                            actionPageItems(
                                type = type,
                                name = name,
                                onNameChange = { name = it },
                                namePlaceholder =
                                    when (type) {
                                        Automation.Type.TRIGGER -> selectedTrigger.title
                                        Automation.Type.STATE -> selectedState.title
                                        Automation.Type.APP -> "Khi mở ứng dụng"
                                        else -> "Quy tắc tự động"
                                    },
                                actions = actions,
                                onActionsChange = { actions = it },
                                entryActions = entryActions,
                                onEntryActionsChange = { entryActions = it },
                                exitActions = exitActions,
                                onExitActionsChange = { exitActions = it },
                                actionTab = actionTab,
                                onActionTabChange = { actionTab = it },
                                onTest = {
                                    HapticUtil.performUIHaptic(view)
                                    scope.launch {
                                        val testList = if (type == Automation.Type.TRIGGER) actions else entryActions
                                        CombinedActionExecutor.executeAll(context, testList)
                                        Toast.makeText(context, "Đã chạy thử chuỗi hành động!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                    }

                    if (pagerState.currentPage != page) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                    .clickable {
                                        HapticUtil.performUIHaptic(view)
                                        scope.launch { pagerState.animateScrollToPage(page) }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector =
                                    if (page > pagerState.currentPage) {
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight
                                    } else {
                                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                                    },
                                contentDescription = "Mở rộng",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
            }

            EssentialsFloatingToolbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                title = if (existing != null) "Chỉnh sửa quy tắc" else "Tạo quy tắc mới",
                onBackClick = ::goBack,
                fabAction = ::save,
                fabIcon = Icons.Rounded.Check,
                fabVisible = type != Automation.Type.APP || selectedApps.isNotEmpty(),
            )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.actionPageItems(
    type: Automation.Type,
    name: String,
    onNameChange: (String) -> Unit,
    namePlaceholder: String,
    actions: List<Action>,
    onActionsChange: (List<Action>) -> Unit,
    entryActions: List<Action>,
    onEntryActionsChange: (List<Action>) -> Unit,
    exitActions: List<Action>,
    onExitActionsChange: (List<Action>) -> Unit,
    actionTab: Int,
    onActionTabChange: (Int) -> Unit,
    onTest: () -> Unit,
) {
    item {
        Text(
            text = "Chọn hành động",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }

    item {
        RoundedCardContainer {
            TextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text(namePlaceholder) },
                label = { Text("Tên quy tắc (Tùy chọn)") },
                singleLine = true,
                shape = RectangleShape,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (type == Automation.Type.TRIGGER) {
        item {
            FlatCard {
                ActionSequenceEditor(actions = actions, onActionsChange = onActionsChange)
            }
        }
    } else {
        item {
            SegmentedPicker(
                items = listOf(0 to "Khi bắt đầu", 1 to "Khi kết thúc"),
                selectedItem = actionTab,
                onItemSelected = onActionTabChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            FlatCard {
                if (actionTab == 0) {
                    ActionSequenceEditor(
                        actions = entryActions,
                        onActionsChange = onEntryActionsChange,
                        emptyMessage = "Chưa có hành động khi bắt đầu.",
                    )
                } else {
                    ActionSequenceEditor(
                        actions = exitActions,
                        onActionsChange = onExitActionsChange,
                        emptyMessage = "Chưa có hành động khi kết thúc.",
                    )
                }
            }
        }
    }

    item {
        RoundedCardContainer {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .clickable(onClick = onTest)
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Chạy thử chuỗi hành động",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FlatCard(content: @Composable () -> Unit) {
    RoundedCardContainer {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(16.dp),
        ) {
            content()
        }
    }
}
