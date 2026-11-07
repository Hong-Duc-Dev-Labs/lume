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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
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
import com.hongducdev.toolkit.car.CarDashboardScreen
import com.hongducdev.toolkit.car.CarMapHttpConfig
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

        // Initialize MapLibre & network config
        MapLibre.getInstance(applicationContext)
        CarMapHttpConfig.initialize(applicationContext)
        MapLibre.setConnected(true)

        val startTab =
            when (intent?.getStringExtra("screen")) {
                "car" -> NavTab.CAR
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
    HOME("Essentials", Icons.Rounded.Home),
    LUNAR("Lịch âm", Icons.Rounded.CalendarMonth),
    ABOUT("About", Icons.Rounded.Info),
    CAR("Car Mode", Icons.Rounded.DirectionsCar),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolkitApp(
    initialTab: NavTab = NavTab.HOME,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var isCarModeActive by rememberSaveable { mutableStateOf(initialTab == NavTab.CAR) }

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

    BackHandler(enabled = isCarModeActive || pagerState.currentPage != 0) {
        if (isCarModeActive) {
            isCarModeActive = false
        } else {
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
    }

    // Full-screen Car Dashboard mode when active
    if (isCarModeActive) {
        CarDashboardScreen(
            onExit = { isCarModeActive = false },
        )
        return
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
                modifier = Modifier.fillMaxSize(),
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
                            onOpenCarDashboard = { isCarModeActive = true },
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

                    NavTab.CAR -> {}
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
                            icon = NavTab.CAR.icon,
                            label = NavTab.CAR.title,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                isCarModeActive = true
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
                    if (isCarModeActive) {
                        2
                    } else {
                        when (pagerState.currentPage) {
                            0 -> 0
                            1 -> 1
                            2 -> 3
                            else -> 0
                        }
                    },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
