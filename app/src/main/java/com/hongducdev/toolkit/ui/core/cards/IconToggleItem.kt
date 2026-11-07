package com.hongducdev.toolkit.ui.core.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hongducdev.toolkit.utils.ColorUtil
import com.hongducdev.toolkit.utils.HapticUtil

/**
 * Standard settings item composable with leading pastel circle icon, title,
 * supporting text, and trailing switch control.
 * Styled identically to sameerasw/essentials ui/core/cards/IconToggleItem.kt.
 */
@Composable
fun IconToggleItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    containerColor: Color? = null,
    iconTint: Color? = null,
    onClick: (() -> Unit)? = null,
    onDisabledClick: (() -> Unit)? = null,
) {
    val pastelBg = ColorUtil.getPastelColorFor(title)
    val vibrantTint = iconTint ?: ColorUtil.getVibrantColorFor(title)
    val view = LocalView.current

    Box(modifier = modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            },
            supportingContent =
                if (!description.isNullOrBlank()) {
                    {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (enabled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                },
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
                                Modifier
                                    .size(40.dp)
                                    .background(color = pastelBg, shape = CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                                tint = vibrantTint,
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
                    if (onClick != null) {
                        VerticalDivider(
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = {
                            if (enabled) {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onCheckedChange(it)
                            }
                        },
                        enabled = enabled,
                    )
                }
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceBright,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (onClick != null && enabled) {
                            Modifier.clickable {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onClick()
                            }
                        } else {
                            Modifier
                        },
                    ),
        )

        if (!enabled && onDisabledClick != null) {
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
                            onDisabledClick()
                        },
            )
        }
    }
}
