package labs.hongducdev.lume.ui.diy.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.domain.diy.Action
import labs.hongducdev.lume.ui.core.containers.RoundedCardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSettingsSheet(
    initialAction: Action.CustomSettings,
    onDismiss: () -> Unit,
    onSave: (Action.CustomSettings) -> Unit,
) {
    var selectedTable by remember { mutableStateOf(initialAction.table) }
    var key by remember { mutableStateOf(initialAction.key) }
    var value by remember { mutableStateOf(initialAction.value) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Tùy biến Cài đặt Hệ thống",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Bảng thiết lập (Namespace)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoundedCardContainer {
                val tables = listOf("system", "secure", "global")
                tables.forEach { table ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedTable = table }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedTable == table,
                            onClick = { selectedTable = table },
                        )
                        Text(
                            text = table.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Tên thuộc tính (Key)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Giá trị (Value)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
                Button(
                    onClick = {
                        onSave(Action.CustomSettings(table = selectedTable, key = key, value = value))
                        onDismiss()
                    },
                ) {
                    Text("Lưu")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
