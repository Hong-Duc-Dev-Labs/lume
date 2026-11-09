package labs.hongducdev.lume.ui.components

import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.ui.core.cards.FeatureTagIcon
import labs.hongducdev.lume.utils.HapticUtil

data class ToolbarItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val badge: (@Composable () -> Unit)? = null,
    val iconScale: Float = 1f,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun EssentialsFloatingToolbar(
    modifier: Modifier = Modifier,
    items: List<ToolbarItem> = emptyList(),
    selectedIndex: Int = -1,
    title: String? = null,
    isBeta: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    fabAction: (() -> Unit)? = null,
    fabIcon: ImageVector? = null,
    fabVisible: Boolean = true,
    floatingActionButton: (@Composable () -> Unit)? = null,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    expanded: Boolean = true,
    backCollapsed: Boolean = false,
    availableWidth: Dp? = null,
) {
    val view = LocalView.current
    val screenWidth = availableWidth ?: LocalConfiguration.current.screenWidthDp.dp
    val maxTitleWidth = (screenWidth - 32.dp - 56.dp - 8.dp - 16.dp - 48.dp - 8.dp).coerceIn(0.dp, 192.dp)
    val collapse by animateFloatAsState(
        targetValue = if (backCollapsed) 1f else 0f,
        animationSpec = tween(380, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "back_collapse",
    )
    var latestTitle by remember { mutableStateOf(title) }
    var latestOnBack by remember { mutableStateOf(onBackClick) }
    if (title != null) latestTitle = title
    if (onBackClick != null) latestOnBack = onBackClick

    // Chiều rộng tiêu đề vừa khít với nội dung (tối đa maxTitleWidth), không cố định
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleMedium
    val titleWidth =
        run {
            val text = latestTitle ?: return@run maxTitleWidth
            val textWidth = with(density) { measurer.measure(text, titleStyle).size.width.toDp() }
            val extra = 16.dp + if (isBeta) 32.dp else 0.dp // padding ngang + tag Beta
            (textWidth + extra + 4.dp).coerceAtMost(maxTitleWidth)
        }

    val finalFab: (@Composable () -> Unit)? =
        when {
            floatingActionButton != null -> {
                floatingActionButton
            }

            onHelpClick != null && fabAction == null -> {
                {
                    FloatingActionButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            onHelpClick()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.large,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = "Trợ giúp",
                        )
                    }
                }
            }

            fabAction != null && fabIcon != null -> {
                {
                    FloatingActionButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            fabAction()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.large,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    ) {
                        Icon(
                            imageVector = fabIcon,
                            contentDescription = null,
                        )
                    }
                }
            }

            else -> {
                null
            }
        }

    val toolbarModifier =
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    val toolbarColors =
        FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
            toolbarContainerColor = MaterialTheme.colorScheme.primary,
        )

    val toolbarContent: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = onBackClick != null,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith
                            (fadeOut() + scaleOut(targetScale = 0.85f)) using SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.Center,
                    label = "toolbar_mode",
                ) { isBackMode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isBackMode) {
                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    latestOnBack?.invoke()
                                },
                                modifier =
                                    Modifier
                                        .sharedBounds(
                                            rememberSharedContentState(key = "toolbar_pill"),
                                            this@AnimatedContent,
                                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                        ).height(48.dp)
                                        .width(48.dp + (8.dp + titleWidth) * if (latestTitle != null) collapse else 0f),
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        containerColor = MaterialTheme.colorScheme.background,
                                    ),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Quay lại",
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            if (latestTitle != null) {
                                Spacer(modifier = Modifier.width(8.dp * (1f - collapse)))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    modifier =
                                        Modifier
                                            .width(titleWidth * (1f - collapse))
                                            .clipToBounds()
                                            .graphicsLayer { alpha = 1f - collapse }
                                            .padding(horizontal = 8.dp)
                                            .align(Alignment.CenterVertically),
                                ) {
                                    AnimatedContent(
                                        targetState = latestTitle,
                                        transitionSpec = {
                                            (fadeIn() + slideInVertically { it / 2 }) togetherWith
                                                (fadeOut() + slideOutVertically { -it / 2 }) using SizeTransform(clip = false)
                                        },
                                        modifier = Modifier.weight(1f, fill = false),
                                        contentAlignment = Alignment.Center,
                                        label = "toolbar_title",
                                    ) { shownTitle ->
                                        Text(
                                            text = shownTitle ?: "",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.background,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.basicMarquee(),
                                        )
                                    }
                                    if (isBeta) {
                                        FeatureTagIcon(
                                            icon = Icons.Rounded.Science,
                                            containerColor = MaterialTheme.colorScheme.background,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            label = "Beta",
                                        )
                                    }
                                }
                            }
                        } else {
                            items.forEachIndexed { index, item ->
                                ToolbarTab(
                                    item = item,
                                    isSelected = selectedIndex == index,
                                    expanded = expanded,
                                    isLast = index == items.size - 1,
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        item.onClick()
                                    },
                                )
                            }
                        }
                    }
                }
            }
    }

    // Toolbar luôn là một call site duy nhất; FAB nằm cạnh trong cùng Row và chỉ đổi
    // visibility có animation, nên chuyển tab không làm toolbar bị tạo lại (tránh giật).
    Row(
        modifier = toolbarModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            scrollBehavior = scrollBehavior,
            colors = toolbarColors,
            content = toolbarContent,
        )
        AnimatedVisibility(
            visible = finalFab != null && fabVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.6f) + expandHorizontally(),
            exit = fadeOut() + scaleOut(targetScale = 0.6f) + shrinkHorizontally(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                finalFab?.invoke()
            }
        }
    }
}

private val TabSpring = spring<Dp>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

@Composable
private fun ToolbarTab(
    item: ToolbarItem,
    isSelected: Boolean,
    expanded: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val labelWidth =
        remember(item.label, labelStyle) {
            with(density) { measurer.measure(item.label, labelStyle).size.width.toDp() }
        }
    val selectedWidth = 24.dp + 16.dp + 8.dp + 16.dp + labelWidth

    val width by animateDpAsState(
        targetValue =
            when {
                isSelected -> selectedWidth
                expanded -> 48.dp
                else -> 0.dp
            },
        animationSpec = TabSpring,
        label = "tab_width",
    )
    val gap by animateDpAsState(if (isLast || width == 0.dp) 0.dp else 2.dp, TabSpring, label = "tab_gap")
    val pillColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent,
        tween(250),
        label = "tab_pill",
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
        tween(250),
        label = "tab_content",
    )
    val labelAlpha by animateFloatAsState(if (isSelected) 1f else 0f, tween(if (isSelected) 300 else 120), label = "tab_label")

    if (width > 0.dp) {
        Box(
            modifier =
                Modifier
                    .width(width)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(pillColor)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .padding(start = 12.dp),
            ) {
                Box {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp * item.iconScale),
                    )
                    if (item.badge != null) {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-7).dp)) { item.badge.invoke() }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.label,
                    style = labelStyle,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.graphicsLayer { alpha = labelAlpha },
                )
            }
        }
        Spacer(Modifier.width(gap))
    }
}
