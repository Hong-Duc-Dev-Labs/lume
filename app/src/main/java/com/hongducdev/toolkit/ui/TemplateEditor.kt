package com.hongducdev.toolkit.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.lunar.LunarCalendar
import com.hongducdev.toolkit.lunar.LunarFormatter
import java.time.Instant

/**
 * Soạn mẫu hiển thị lịch âm: ô nhập, xem trước, mẫu dựng sẵn và token để chèn.
 * [savedTemplate] là giá trị đã lưu; ô nhập chỉ lấy giá trị này một lần khi mở màn hình.
 */
@Composable
fun TemplateEditor(
    label: String,
    savedTemplate: String,
    onTemplateChange: (String) -> Unit,
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Xem trước", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = preview.ifBlank { "(trống)" },
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        OutlinedTextField(
            value = template,
            onValueChange = ::change,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Mẫu dựng sẵn", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LunarFormatter.presets.forEach { preset ->
                SuggestionChip(onClick = { change(preset.template) }, label = { Text(preset.name) })
            }
        }

        Text("Chèn token (bấm để thêm vào cuối mẫu)", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LunarFormatter.supportedTokens.forEach { token ->
                SuggestionChip(
                    onClick = { change("$template{{$token}}") },
                    label = { Text("{{$token}}") },
                )
            }
        }
    }
}
