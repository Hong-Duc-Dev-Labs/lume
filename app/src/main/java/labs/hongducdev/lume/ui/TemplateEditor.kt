package labs.hongducdev.lume.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.lunar.LunarCalendar
import labs.hongducdev.lume.lunar.LunarFormatter
import labs.hongducdev.lume.ui.theme.ExpressiveRadius
import java.time.Instant

/**
 * Soạn mẫu hiển thị lịch âm: ô nhập, xem trước, mẫu dựng sẵn và token để chèn.
 * Thiết kế theo chuẩn Material 3 Expressive.
 */
@Composable
fun TemplateEditor(
    label: String,
    savedTemplate: String,
    onTemplateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(savedTemplate) {
        if (text == null) text = savedTemplate
    }
    val template = text ?: savedTemplate

    fun change(value: String) {
        text = value
        onTemplateChange(value)
    }

    val preview = remember(template) {
        LunarFormatter.format(template, LunarCalendar.fromInstant(Instant.now()))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Expressive Preview Card
        Card(
            shape = RoundedCornerShape(ExpressiveRadius.Item),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Xem trước trực tiếp",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = preview.ifBlank { "(trống)" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Outlined Input with Clear action
        OutlinedTextField(
            value = template,
            onValueChange = ::change,
            label = { Text(label) },
            shape = RoundedCornerShape(ExpressiveRadius.Squircle),
            singleLine = true,
            trailingIcon = {
                if (template.isNotEmpty()) {
                    IconButton(onClick = { change("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Xóa nội dung",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Presets chips
        Text(
            text = "Mẫu gợi ý dựng sẵn",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LunarFormatter.presets.forEach { preset ->
                SuggestionChip(
                    onClick = { change(preset.template) },
                    label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(ExpressiveRadius.Pill),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    border = null,
                )
            }
        }

        // Token chips
        Text(
            text = "Chèn nhanh token",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LunarFormatter.supportedTokens.forEach { token ->
                SuggestionChip(
                    onClick = { change("$template{{$token}}") },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    label = { Text("{{$token}}", style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(ExpressiveRadius.Pill),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    border = null,
                )
            }
        }
    }
}
