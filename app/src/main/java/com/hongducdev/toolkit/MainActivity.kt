package com.hongducdev.toolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.ui.modifiers.progressiveBlur
import com.hongducdev.toolkit.ui.modifiers.scrollMotionBlur
import com.hongducdev.toolkit.car.CarMapHttpConfig
import com.hongducdev.toolkit.car.auto.AndroidAutoPhoneScreen
import com.hongducdev.toolkit.ui.LockscreenLunarScreen
import com.hongducdev.toolkit.ui.components.EssentialsFloatingToolbar
import com.hongducdev.toolkit.ui.components.ToolbarItem
import com.hongducdev.toolkit.ui.screens.AboutScreen
import com.hongducdev.toolkit.ui.screens.HomeScreen
import com.hongducdev.toolkit.ui.theme.ToolkitTheme
import com.hongducdev.toolkit.utils.HapticUtil
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Khởi tạo MapLibre & cấu hình mạng cho bản đồ của màn hình Android Auto
        MapLibre.getInstance(applicationContext)
        CarMapHttpConfig.initialize(applicationContext)
        MapLibre.setConnected(true)

        val startTab =
            when (intent?.getStringExtra("screen")) {
                "lunar" -> NavTab.LUNAR
                else -> NavTab.HOME
            }

        setContent {
            ToolkitTheme(
                darkTheme = isSystemInDarkTheme(),
                pitchBlackTheme = true,
                dynamicColor = true,
            ) {
                ToolkitApp(initialTab = startTab)
            }
        }
    }
}

enum class NavTab(
    val title: String,
    val icon: ImageVector,
) {
    HOME("Trang chủ", Icons.Rounded.Home),
    LUNAR("Lịch âm", Icons.Rounded.CalendarMonth),
    ABOUT("About", Icons.Rounded.Info),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolkitApp(
    initialTab: NavTab = NavTab.HOME,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var androidAutoOpen by rememberSaveable { mutableStateOf(false) }
    if (androidAutoOpen) {
        AndroidAutoPhoneScreen(onExit = { androidAutoOpen = false })
        return
    }

    val mainTabs = remember { listOf(NavTab.HOME, NavTab.LUNAR, NavTab.ABOUT) }
    val initialPage =
        remember {
            val idx = mainTabs.indexOf(initialTab)
            if (idx >= 0) idx else 0
        }

    val pagerState =
        rememberPagerState(
            initialPage = initialPage,
            pageCount = { mainTabs.size },
        )

    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch {
            pagerState.animateScrollToPage(
                0,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
            )
        }
    }

    // Bucketed rumble haptic feedback while swiping between tabs (Essentials style)
    var lastSwipeHapticBucket by remember { mutableIntStateOf(0) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPageOffsetFraction }
            .collect { offset ->
                val fraction = abs(offset)
                val currentBucket = (fraction * 10f).toInt()
                if (currentBucket != lastSwipeHapticBucket) {
                    lastSwipeHapticBucket = currentBucket
                    HapticUtil.performSliderHaptic(view)
                }
            }
    }

    // Heavy haptic when landing on a new tab
    LaunchedEffect(pagerState) {
        var isFirst = true
        snapshotFlow { pagerState.currentPage }
            .collect {
                if (isFirst) {
                    isFirst = false
                } else {
                    HapticUtil.performHeavyHaptic(view)
                }
            }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val contentPadding = PaddingValues(top = topInset)

    val density = LocalDensity.current
    val topBlurPx = with(density) { (topInset + 28.dp).toPx() }
    val bottomBlurPx =
        with(density) { (WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 128.dp).toPx() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {},
        bottomBar = {},
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // HorizontalPager allowing smooth gestures between tabs
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .progressiveBlur(topBlurPx, bottomBlurPx)
                        .scrollMotionBlur(pagerState),
            ) { page ->
                when (mainTabs[page]) {
                    NavTab.HOME -> {
                        HomeScreen(
                            contentPadding = contentPadding,
                            onOpenLunar = {
                                scope.launch {
                                    val lunarIdx = mainTabs.indexOf(NavTab.LUNAR)
                                    if (lunarIdx >= 0) pagerState.animateScrollToPage(lunarIdx)
                                }
                            },
                            onOpenAndroidAuto = { androidAutoOpen = true },
                        )
                    }

                    NavTab.LUNAR -> {
                        LockscreenLunarScreen(
                            contentPadding = contentPadding,
                        )
                    }

                    NavTab.ABOUT -> {
                        AboutScreen(
                            contentPadding = contentPadding,
                        )
                    }

                }
            }

            // Material 3 Expressive Horizontal Floating Toolbar
            EssentialsFloatingToolbar(
                items =
                    listOf(
                        ToolbarItem(
                            icon = NavTab.HOME.icon,
                            label = NavTab.HOME.title,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        0,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                    )
                                }
                            },
                        ),
                        ToolbarItem(
                            icon = NavTab.LUNAR.icon,
                            label = NavTab.LUNAR.title,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        1,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                    )
                                }
                            },
                        ),
                        ToolbarItem(
                            icon = NavTab.ABOUT.icon,
                            label = NavTab.ABOUT.title,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        2,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                    )
                                }
                            },
                        ),
                    ),
                selectedIndex =
                    pagerState.currentPage.coerceIn(0, mainTabs.lastIndex),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
