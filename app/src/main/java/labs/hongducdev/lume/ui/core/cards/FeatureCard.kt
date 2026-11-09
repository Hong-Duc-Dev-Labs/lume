package labs.hongducdev.lume.ui.core.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import labs.hongducdev.lume.ui.components.menus.SegmentedDropdownMenu
import labs.hongducdev.lume.ui.components.menus.SegmentedDropdownMenuItem
import labs.hongducdev.lume.ui.state.LocalMenuStateManager
import labs.hongducdev.lume.utils.ColorUtil
import labs.hongducdev.lume.utils.HapticUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeatureCard(
    title: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    hasMoreSettings: Boolean = true,
    isToggleEnabled: Boolean = true,
    showToggle: Boolean = true,
    onDisabledToggleClick: (() -> Unit)? = null,
    isBeta: Boolean = false,
    isUnsupported: Boolean = false,
    isLegacy: Boolean = false,
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    isSelected: Boolean = false,
    iconSize: Dp = 24.dp,
    hasIconBackground: Boolean = true,
    iconTint: Color? = null,
) {
    val selectedContainer by animateColorAsState(
        targetValue =
            if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                containerColor ?: MaterialTheme.colorScheme.surfaceBright
            },
        label = "feature_card_selected",
    )
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    val menuState = LocalMenuStateManager.current
    DisposableEffect(showMenu) {
        if (showMenu) {
            menuState.activeId = title
        } else if (menuState.activeId == title) {
            menuState.activeId = null
        }
        onDispose {
            if (menuState.activeId == title) {
                menuState.activeId = null
            }
        }
    }

    val isBlurred = menuState.activeId != null && menuState.activeId != title
    val blurRadius by animateDpAsState(
        targetValue = if (isBlurred) 10.dp else 0.dp,
        animationSpec = tween(500),
        label = "blur",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isBlurred) 0.5f else 1f,
        animationSpec = tween(500),
        label = "alpha",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(alpha)
                .blur(blurRadius),
    ) {
        ListItem(
            headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isBeta) {
                        FeatureTagIcon(
                            icon = Icons.Rounded.Science,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            label = "Beta",
                        )
                    }
                    if (isUnsupported) {
                        FeatureTagIcon(
                            icon = Icons.Rounded.Dangerous,
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            label = "Unsupported",
                        )
                    }
                    if (isLegacy) {
                        FeatureTagIcon(
                            icon = Icons.Rounded.Archive,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            label = "Legacy",
                        )
                    }
                }
            },
            supportingContent =
                if (!description.isNullOrBlank()) {
                    {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    null
                },
            leadingContent =
                if (icon != null) {
                    {
                        Box(
                            modifier =
                                if (hasIconBackground) {
                                    Modifier
                                        .size(40.dp)
                                        .background(ColorUtil.getPastelColorFor(title), CircleShape)
                                } else {
                                    Modifier.size(40.dp)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(iconSize),
                                tint = iconTint ?: ColorUtil.getVibrantColorFor(title),
                            )
                        }
                    }
                } else {
                    null
                },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (showToggle && hasMoreSettings) {
                        VerticalDivider(
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    if (showToggle) {
                        Box {
                            Switch(
                                checked = if (isToggleEnabled) isEnabled else false,
                                onCheckedChange = { checked ->
                                    if (isToggleEnabled) {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        onToggle(checked)
                                    }
                                },
                                enabled = isToggleEnabled,
                            )

                            if (!isToggleEnabled && onDisabledToggleClick != null) {
                                Box(
                                    modifier =
                                        Modifier
                                            .matchParentSize()
                                            .zIndex(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                onDisabledToggleClick()
                                            },
                                )
                            }
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = selectedContainer),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            if (!isToggleEnabled && !hasMoreSettings && onDisabledToggleClick != null) {
                                onDisabledToggleClick()
                            } else {
                                onClick()
                            }
                        },
                        onLongClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showMenu = true
                        },
                    ),
        )

        SegmentedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            if (onPinToggle != null) {
                SegmentedDropdownMenuItem(
                    text = { Text(if (isPinned) "Bỏ ghim" else "Ghim lên đầu") },
                    onClick = {
                        showMenu = false
                        onPinToggle()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isPinned) Icons.Rounded.BookmarkRemove else Icons.Rounded.Bookmark,
                            contentDescription = null,
                        )
                    },
                )
            }

            if (onHelpClick != null) {
                SegmentedDropdownMenuItem(
                    text = { Text("Tính năng này là gì?") },
                    onClick = {
                        showMenu = false
                        onHelpClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}
