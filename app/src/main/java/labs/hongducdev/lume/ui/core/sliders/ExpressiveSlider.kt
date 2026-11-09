package labs.hongducdev.lume.ui.core.sliders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.ui.theme.ExpressiveRadius
import labs.hongducdev.lume.utils.HapticUtil

/**
 * Expressive Slider with value badge pill, -/+ steppers and haptics,
 * following Material 3 guidelines and sameerasw/essentials.
 */
@Composable
fun ExpressiveSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueFormatted: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = (range.endInclusive - range.start) / 20f,
) {
    val view = LocalView.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Live value badge pill
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(ExpressiveRadius.Pill))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = valueFormatted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Decrement Button (-)
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    val newValue = (value - step).coerceIn(range)
                    onValueChange(newValue)
                },
                modifier = Modifier.size(36.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Giảm",
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Slider with haptic tick on drag
            Slider(
                value = value.coerceIn(range),
                onValueChange = { newValue ->
                    if (newValue != value) {
                        HapticUtil.performSliderHaptic(view)
                        onValueChange(newValue)
                    }
                },
                valueRange = range,
                modifier = Modifier.weight(1f),
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
            )

            Spacer(Modifier.width(8.dp))

            // Increment Button (+)
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    val newValue = (value + step).coerceIn(range)
                    onValueChange(newValue)
                },
                modifier = Modifier.size(36.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Tăng",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
