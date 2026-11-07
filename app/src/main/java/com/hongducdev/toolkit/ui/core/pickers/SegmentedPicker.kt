package com.hongducdev.toolkit.ui.core.pickers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.ui.theme.ExpressiveRadius
import com.hongducdev.toolkit.utils.HapticUtil

/**
 * Expressive Segmented Picker inspired by Material 3 and sameerasw/essentials.
 */
@Composable
fun <T> SegmentedPicker(
    items: List<Pair<T, String>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ExpressiveRadius.Pill))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
    ) {
        items.forEach { (item, label) ->
            val isSelected = item == selectedItem
            val targetBgColor =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            val targetTextColor =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

            val animatedBg by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(200), label = "bg")
            val animatedTextColor by animateColorAsState(targetValue = targetTextColor, animationSpec = tween(200), label = "text")

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ExpressiveRadius.Pill))
                        .background(animatedBg)
                        .semantics { role = Role.RadioButton }
                        .clickable {
                            if (!isSelected) {
                                HapticUtil.performUIHaptic(view)
                                onItemSelected(item)
                            }
                        }.padding(vertical = 10.dp, horizontal = 12.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = animatedTextColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
