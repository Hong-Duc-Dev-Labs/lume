package labs.hongducdev.lume.ui.core.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.utils.ColorUtil
import labs.hongducdev.lume.utils.HapticUtil

/**
 * Clickable settings row styled as an M3 ListItem with pastel leading circle icon,
 * title, description, trailing value/chip, and chevron.
 * Styled identically to sameerasw/essentials.
 */
@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    valueText: String? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    containerColor: Color? = null,
    iconTint: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val pastelBg = ColorUtil.getPastelColorFor(title)
    val vibrantTint = iconTint ?: ColorUtil.getVibrantColorFor(title)
    val view = LocalView.current

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
            if (!description.isNullOrBlank() || !valueText.isNullOrBlank()) {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!description.isNullOrBlank()) {
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
                        if (!valueText.isNullOrBlank()) {
                            Text(
                                text = valueText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier =
                                    Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(50),
                                        ).padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
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
        trailingContent =
            if (showChevron && onClick != null) {
                {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else {
                null
            },
        colors =
            ListItemDefaults.colors(
                containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceBright,
            ),
        modifier =
            modifier
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
}
